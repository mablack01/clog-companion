package com.clogcompanion.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Value;

/**
 * Skill, quest and diary requirements for a collection log source. Names are the
 * {@code Skill}, {@code Quest} and {@code Varbits.DIARY_*} constant names.
 */
@Value
public class Requirements
{
	Map<String, Integer> skills;
	List<String> quests;
	List<String> diaries;

	public static Requirements none()
	{
		return new Requirements(Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
	}

	public Map<String, Integer> getSkills()
	{
		return skills == null ? Collections.emptyMap() : skills;
	}

	public List<String> getQuests()
	{
		return quests == null ? Collections.emptyList() : quests;
	}

	public List<String> getDiaries()
	{
		return diaries == null ? Collections.emptyList() : diaries;
	}
}
