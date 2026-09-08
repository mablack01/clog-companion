package com.clogcompanion;

import com.clogcompanion.account.AccountStateReader;
import com.clogcompanion.data.ClogDataset;
import com.clogcompanion.engine.ClogFilter;
import com.clogcompanion.engine.DifficultyEngine;
import com.clogcompanion.engine.RatedSlot;
import com.clogcompanion.engine.SlotRater;
import com.clogcompanion.model.AccountState;
import com.clogcompanion.ui.ClogPanel;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Clog Companion",
	description = "Ranks Collection Log slots by how quick they are to get",
	tags = {"collection log", "clog", "completionist", "drop rate"}
)
public class ClogCompanionPlugin extends Plugin
{
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
	private ClogFilter filter;
	private ClogPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		dataset = ClogDataset.load(gson);
		log.debug("Clog Companion loaded {} slots across {} entries", dataset.getItems().size(), dataset.getSources().size());
		filter = new ClogFilter();
		filter.setHideObtained(config.hideObtained());
		filter.setOnlyMeetsRequirements(config.onlyMeetsRequirements());
		panel = new ClogPanel(itemManager, filter, this::persistFilter);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Clog Companion")
			.icon(ImageUtil.resizeImage(icon, 16, 16))
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		refresh();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		dataset = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN || event.getGameState() == GameState.LOGIN_SCREEN)
		{
			refresh();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (ClogCompanionConfig.GROUP.equals(event.getGroup()))
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
			boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
			AccountState state = loggedIn ? AccountStateReader.read(client) : AccountState.empty();
			List<RatedSlot> rated = new SlotRater(dataset, engine()).rateAll(state, Collections.emptySet());
			String status = loggedIn ? "Requirements checked against this account" : "Log in to check requirements";
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.setStatus(status);
					panel.setSlots(rated);
				}
			});
		});
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
