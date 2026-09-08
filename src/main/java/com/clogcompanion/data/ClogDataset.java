package com.clogcompanion.data;

import com.clogcompanion.model.ClogItem;
import com.clogcompanion.model.ClogSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/** The bundled collection log: every slot joined to its curated source. */
public class ClogDataset
{
	private static class ItemsFile
	{
		List<ClogItem> items;
	}

	@Getter
	private final List<ClogItem> items;
	@Getter
	private final Map<String, ClogSource> sources;
	private final Map<String, ClogItem> byId = new HashMap<>();
	private final Map<String, List<ClogItem>> byName = new HashMap<>();

	ClogDataset(List<ClogItem> items, List<ClogSource> sourceList)
	{
		this.items = Collections.unmodifiableList(new ArrayList<>(items));
		Map<String, ClogSource> map = new LinkedHashMap<>();
		for (ClogSource s : sourceList)
		{
			map.put(s.getId(), s);
		}
		this.sources = Collections.unmodifiableMap(map);
		for (ClogItem item : items)
		{
			byId.put(item.getId(), item);
			byName.computeIfAbsent(item.getName().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(item);
		}
	}

	public static ClogDataset load(Gson gson)
	{
		try (Reader items = open("clog-items.json"); Reader sources = open("clog-sources.json"))
		{
			ItemsFile file = gson.fromJson(items, ItemsFile.class);
			List<ClogSource> list = gson.fromJson(sources, new TypeToken<List<ClogSource>>()
			{
			}.getType());
			return new ClogDataset(file.items, list);
		}
		catch (IOException | RuntimeException e)
		{
			throw new IllegalStateException("Clog Companion data failed to load", e);
		}
	}

	private static Reader open(String name) throws IOException
	{
		InputStream in = ClogDataset.class.getResourceAsStream("/com/clogcompanion/" + name);
		if (in == null)
		{
			throw new IOException("missing resource " + name);
		}
		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}

	/** The entry the slot is listed under. */
	public ClogSource sourceOf(ClogItem item)
	{
		return sources.get(item.getSourceId());
	}

	/** The source whose per-attempt time applies to the slot (differs from {@link #sourceOf} for catch-all entries). */
	public ClogSource timeSourceOf(ClogItem item)
	{
		String id = item.getTimeSourceId();
		return id == null ? sourceOf(item) : sources.getOrDefault(id, sourceOf(item));
	}

	public Optional<ClogItem> byId(String id)
	{
		return Optional.ofNullable(byId.get(id));
	}

	/** Every slot sharing an item name; the game marks all of them when one is obtained. */
	public List<ClogItem> byName(String itemName)
	{
		return byName.getOrDefault(itemName.toLowerCase(Locale.ROOT), Collections.emptyList());
	}

	/** Dangling references and unknown constant names; empty when the data is consistent. */
	public List<String> validate()
	{
		List<String> problems = new ArrayList<>();
		for (ClogItem item : items)
		{
			if (byId.get(item.getId()) != item)
			{
				problems.add(item.getId() + ": duplicate id");
			}
			if (!sources.containsKey(item.getSourceId()))
			{
				problems.add(item.getId() + ": unknown sourceId " + item.getSourceId());
			}
			if (item.getTimeSourceId() != null && !sources.containsKey(item.getTimeSourceId()))
			{
				problems.add(item.getId() + ": unknown timeSourceId " + item.getTimeSourceId());
			}
		}
		for (ClogSource s : sources.values())
		{
			if (s.getCategory() == null)
			{
				problems.add(s.getId() + ": unknown category");
			}
			for (String skill : s.getRequirements().getSkills().keySet())
			{
				if (!isEnum(Skill.class, skill))
				{
					problems.add(s.getId() + ": unknown skill " + skill);
				}
			}
			for (String quest : s.getRequirements().getQuests())
			{
				if (!isEnum(Quest.class, quest))
				{
					problems.add(s.getId() + ": unknown quest " + quest);
				}
			}
			for (String diary : s.getRequirements().getDiaries())
			{
				if (!Diaries.BY_NAME.containsKey(diary))
				{
					problems.add(s.getId() + ": unknown diary " + diary);
				}
			}
		}
		return problems;
	}

	private static <E extends Enum<E>> boolean isEnum(Class<E> type, String name)
	{
		try
		{
			Enum.valueOf(type, name);
			return true;
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
	}
}
