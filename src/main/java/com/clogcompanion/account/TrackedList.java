package com.clogcompanion.account;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.runelite.client.config.ConfigManager;

/** Slots the player has chosen to go for, persisted per RuneScape profile. */
public class TrackedList
{
	static final String GROUP = "clog-companion";
	static final String KEY = "trackedSlots";

	private final ConfigManager configManager;
	private final Gson gson;
	private final Set<String> slotIds = new LinkedHashSet<>();

	public TrackedList(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	public boolean contains(String slotId)
	{
		return slotIds.contains(slotId);
	}

	public Set<String> ids()
	{
		return Collections.unmodifiableSet(slotIds);
	}

	public int size()
	{
		return slotIds.size();
	}

	/** Adds if absent, removes if present; returns true when the slot is now tracked. */
	public boolean toggle(String slotId)
	{
		boolean tracked = slotIds.add(slotId);
		if (!tracked)
		{
			slotIds.remove(slotId);
		}
		save();
		return tracked;
	}

	public void add(String slotId)
	{
		if (slotIds.add(slotId))
		{
			save();
		}
	}

	/** Drops every tracked slot the player now owns; returns how many were removed. */
	public int clearCompleted(Set<String> obtainedSlotIds)
	{
		int before = slotIds.size();
		if (slotIds.removeAll(obtainedSlotIds))
		{
			save();
		}
		return before - slotIds.size();
	}

	public void load()
	{
		slotIds.clear();
		Set<String> saved = configManager.getRSProfileConfiguration(GROUP, KEY, new TypeToken<Set<String>>()
		{
		}.getType());
		if (saved != null)
		{
			slotIds.addAll(saved);
		}
	}

	public void clear()
	{
		slotIds.clear();
	}

	private void save()
	{
		configManager.setRSProfileConfiguration(GROUP, KEY, gson.toJson(slotIds));
	}
}
