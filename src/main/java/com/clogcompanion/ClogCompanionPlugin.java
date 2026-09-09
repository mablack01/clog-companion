package com.clogcompanion;

import com.clogcompanion.account.AccountStateReader;
import com.clogcompanion.account.ObtainedTracker;
import com.clogcompanion.account.TrackedList;
import com.clogcompanion.data.ClogDataset;
import com.clogcompanion.data.Diaries;
import com.clogcompanion.engine.ClogFilter;
import com.clogcompanion.engine.DifficultyEngine;
import com.clogcompanion.engine.RatedSlot;
import com.clogcompanion.engine.SlotRater;
import com.clogcompanion.model.AccountState;
import com.clogcompanion.ui.ClogPanel;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Clog Companion",
	description = "Ranks Collection Log slots by how quick they are to get",
	tags = {"collection log", "clog", "completionist", "drop rate"}
)
public class ClogCompanionPlugin extends Plugin
{
	/** Clientscript the game runs once per owned item while populating the collection log interface. */
	private static final int COLLECTION_ITEM_SCRIPT = 4100;
	/** Pinned target, stored per RuneScape profile like the owned set. */
	private static final String PINNED_KEY = "pinnedSlot";
	/** Config keys that change ratings; other keys in the group are UI state or our own persistence. */
	private static final Set<String> RATING_KEYS = new HashSet<>(Arrays.asList(
		"estimateMode", "easyMaxMinutes", "mediumMaxMinutes", "longMaxMinutes"));

	@Inject
	private ClogCompanionConfig config;
	@Inject
	private ConfigManager configManager;
	@Inject
	private Gson gson;
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ItemManager itemManager;

	@Getter
	private ClogDataset dataset;
	private ObtainedTracker obtained;
	private TrackedList tracked;
	private ClogFilter filter;
	private ClogPanel panel;
	private NavigationButton navButton;
	private boolean accountDirty;

	@Override
	protected void startUp()
	{
		dataset = ClogDataset.load(gson);
		log.debug("Clog Companion loaded {} slots across {} entries", dataset.getItems().size(), dataset.getSources().size());
		obtained = new ObtainedTracker(dataset, configManager, gson);
		tracked = new TrackedList(configManager, gson);
		filter = new ClogFilter();
		filter.setHideObtained(config.hideObtained());
		filter.setOnlyMeetsRequirements(config.onlyMeetsRequirements());
		panel = new ClogPanel(itemManager, filter, tracked, this::persistFilter, this::pin, () -> pin(null));
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Clog Companion")
			.icon(ImageUtil.resizeImage(icon, 16, 16))
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			obtained.load();
			tracked.load();
		}
		refresh();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel.closeRoll();
		panel = null;
		filter = null;
		obtained = null;
		tracked = null;
		dataset = null;
	}

	/** Owned items are per account: (re)load when RuneLite resolves the profile, clear when it goes away. */
	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		boolean gone = event.getNewProfile() == null;
		if (gone)
		{
			obtained.clear();
		}
		else
		{
			obtained.load();
		}
		// The tracked list is EDT-owned (row buttons mutate it); queue before refresh() queues its own EDT work.
		SwingUtilities.invokeLater(() -> trackedProfile(gone));
		refresh();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			obtained.clear();
			SwingUtilities.invokeLater(() -> trackedProfile(true));
			refresh();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (ClogCompanionConfig.GROUP.equals(event.getGroup()) && RATING_KEYS.contains(event.getKey()))
		{
			refresh();
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() != COLLECTION_ITEM_SCRIPT || event.getScriptEvent() == null)
		{
			return;
		}
		Object[] args = event.getScriptEvent().getArguments();
		if (args != null && args.length > 1 && args[1] instanceof Integer)
		{
			int itemId = (Integer) args[1];
			obtained.markItemId(itemId);
			obtained.markItemName(itemManager.getItemComposition(itemId).getName());
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		accountDirty = true;
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (Diaries.BY_NAME.containsValue(event.getVarbitId()))
		{
			accountDirty = true;
		}
	}

	/** Script bursts and stat storms both settle here: persist and re-rate at most once per tick. */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		boolean synced = obtained.flush();
		if (synced || accountDirty)
		{
			accountDirty = false;
			refresh();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.GAMEMESSAGE && obtained.onChatMessage(Text.removeTags(event.getMessage())))
		{
			refresh();
		}
	}

	public DifficultyEngine engine()
	{
		int easy = config.easyMaxMinutes();
		int medium = Math.max(easy, config.mediumMaxMinutes());
		int longMax = Math.max(medium, config.longMaxMinutes());
		return new DifficultyEngine(config.estimateMode(), easy, medium, longMax);
	}

	/** Re-rates every slot against the current account on the client thread, then updates the panel. */
	private void refresh()
	{
		clientThread.invokeLater(() ->
		{
			ClogDataset data = dataset;
			ObtainedTracker owned = obtained;
			if (data == null || owned == null)
			{
				return;
			}
			boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
			AccountState state = loggedIn ? AccountStateReader.read(client) : AccountState.empty();
			List<RatedSlot> rated = new SlotRater(data, engine()).rateAll(state, owned.obtainedSlotIds());
			String status = !loggedIn ? "Log in to check requirements"
				: owned.isSynced() ? "Synced with this account's log" : "Open your Collection Log once to sync";
			String pinnedId = configManager.getRSProfileConfiguration(ClogCompanionConfig.GROUP, PINNED_KEY);
			RatedSlot pinnedSlot = rated.stream().filter(r -> r.getItem().getId().equals(pinnedId)).findFirst().orElse(null);
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.setStatus(status);
					panel.setPinned(pinnedSlot);
					panel.setSlots(rated);
				}
			});
		});
	}

	private void trackedProfile(boolean gone)
	{
		TrackedList list = tracked;
		if (list == null)
		{
			return;
		}
		if (gone)
		{
			list.clear();
		}
		else
		{
			list.load();
		}
	}

	private void pin(RatedSlot slot)
	{
		if (slot == null)
		{
			configManager.unsetRSProfileConfiguration(ClogCompanionConfig.GROUP, PINNED_KEY);
		}
		else
		{
			tracked.add(slot.getItem().getId());
			configManager.setRSProfileConfiguration(ClogCompanionConfig.GROUP, PINNED_KEY, slot.getItem().getId());
		}
		refresh();
	}

	private void persistFilter()
	{
		configManager.setConfiguration(ClogCompanionConfig.GROUP, "hideObtained", filter.isHideObtained());
		configManager.setConfiguration(ClogCompanionConfig.GROUP, "onlyMeetsRequirements", filter.isOnlyMeetsRequirements());
	}

	@Provides
	ClogCompanionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClogCompanionConfig.class);
	}
}
