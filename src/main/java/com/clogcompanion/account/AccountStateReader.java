package com.clogcompanion.account;

import com.clogcompanion.data.Diaries;
import com.clogcompanion.model.AccountState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/** Snapshots the logged-in account. Must run on the client thread. */
public final class AccountStateReader
{
	private AccountStateReader()
	{
	}

	public static AccountState read(Client client)
	{
		Map<String, Integer> levels = new HashMap<>();
		for (Skill skill : Skill.values())
		{
			levels.put(skill.name(), client.getRealSkillLevel(skill));
		}
		Set<String> quests = new HashSet<>();
		for (Quest quest : Quest.values())
		{
			if (quest.getState(client) == QuestState.FINISHED)
			{
				quests.add(quest.name());
			}
		}
		Set<String> diaries = new HashSet<>();
		for (Map.Entry<String, Integer> e : Diaries.BY_NAME.entrySet())
		{
			if (client.getVarbitValue(e.getValue()) == 1)
			{
				diaries.add(e.getKey());
			}
		}
		return new AccountState(levels, quests, diaries);
	}
}
