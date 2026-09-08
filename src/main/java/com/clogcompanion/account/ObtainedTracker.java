package com.clogcompanion.account;

import com.clogcompanion.data.ClogDataset;
import com.clogcompanion.model.ClogItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Which slots the current account owns, fed by the game's collection log population script
 * (one call per owned item when the log is opened) and the "New item added" chat line,
 * persisted per RuneScape profile.
 */
@Slf4j
public class ObtainedTracker
{
	static final String GROUP = "clog-companion";
	static final String KEY = "obtainedItemIds";
	private static final Pattern NEW_ITEM = Pattern.compile("New item added to your collection log: (.+)");

	private final ClogDataset dataset;
	private final ConfigManager configManager;
	private final Gson gson;
	private final Set<Integer> itemIds = new HashSet<>();
	private boolean synced;
	private boolean dirty;

	public ObtainedTracker(ClogDataset dataset, ConfigManager configManager, Gson gson)
	{
		this.dataset = dataset;
		this.configManager = configManager;
		this.gson = gson;
	}

	/** True once the whole log has been read at least once for this profile. */
	public boolean isSynced()
	{
		return synced;
	}

	public Set<String> obtainedSlotIds()
	{
		return dataset.getItems().stream().filter(i -> itemIds.contains(i.getItemId())).map(ClogItem::getId).collect(Collectors.toSet());
	}

	/** From the game's collection log population script; call {@link #flush()} once the burst ends. */
	public boolean markItemId(int id)
	{
		synced = true;
		dirty |= itemIds.add(id);
		return dirty;
	}

	/** Persists pending marks; returns true if anything was written. */
	public boolean flush()
	{
		if (!dirty)
		{
			return false;
		}
		dirty = false;
		save();
		return true;
	}

	/** Handles the "New item added to your collection log: X" game message; returns true if it matched. */
	public boolean onChatMessage(String message)
	{
		Matcher m = NEW_ITEM.matcher(message);
		if (!m.find())
		{
			return false;
		}
		boolean changed = false;
		for (ClogItem item : dataset.byName(m.group(1).trim()))
		{
			changed |= itemIds.add(item.getItemId());
		}
		if (changed)
		{
			save();
		}
		return changed;
	}

	public void load()
	{
		itemIds.clear();
		Set<Integer> saved = configManager.getRSProfileConfiguration(GROUP, KEY, new TypeToken<Set<Integer>>()
		{
		}.getType());
		if (saved != null)
		{
			itemIds.addAll(saved);
		}
		synced = !itemIds.isEmpty();
		log.debug("Loaded {} obtained item ids", itemIds.size());
	}

	public void clear()
	{
		itemIds.clear();
		synced = false;
	}

	private void save()
	{
		configManager.setRSProfileConfiguration(GROUP, KEY, gson.toJson(itemIds));
	}

	Set<Integer> itemIds()
	{
		return Collections.unmodifiableSet(itemIds);
	}
}
