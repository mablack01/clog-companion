package com.clogcompanion.engine;

import com.clogcompanion.model.AccountState;
import com.clogcompanion.model.Requirements;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RequirementChecker
{
	private RequirementChecker()
	{
	}

	/** Human-readable reasons the account does not meet {@code req}; empty means met. */
	public static List<String> unmet(Requirements req, AccountState state)
	{
		List<String> out = new ArrayList<>();
		for (Map.Entry<String, Integer> e : req.getSkills().entrySet())
		{
			int have = state.getSkillLevels().getOrDefault(e.getKey(), 1);
			if (have < e.getValue())
			{
				out.add(titleCase(e.getKey()) + " " + e.getValue() + " (have " + have + ")");
			}
		}
		for (String quest : req.getQuests())
		{
			if (!state.getCompletedQuests().contains(quest))
			{
				out.add(titleCase(quest) + " not completed");
			}
		}
		for (String diary : req.getDiaries())
		{
			if (!state.getCompletedDiaries().contains(diary))
			{
				out.add(titleCase(diary.replaceFirst("^DIARY_", "")) + " diary not completed");
			}
		}
		return out;
	}

	static String titleCase(String constant)
	{
		StringBuilder sb = new StringBuilder();
		for (String word : constant.toLowerCase(Locale.ROOT).split("_"))
		{
			if (word.isEmpty())
			{
				continue;
			}
			if (sb.length() > 0)
			{
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
		}
		return sb.toString();
	}
}
