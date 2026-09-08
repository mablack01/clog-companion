package com.clogcompanion.account;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.clogcompanion.model.AccountState;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

public class AccountStateReaderTest
{
	@Test
	public void readsLevelsQuestsAndDiaries()
	{
		Client client = mock(Client.class);
		when(client.getRealSkillLevel(Skill.SLAYER)).thenReturn(93);
		when(client.getVarbitValue(VarbitID.DESERT_DIARY_HARD_COMPLETE)).thenReturn(1);
		// Quest.getState runs QUEST_STATUS_GET and reads the int stack: 2 = finished, 1 = not started.
		int[] stack = new int[1];
		doAnswer(inv ->
		{
			stack[0] = (int) inv.getArgument(1) == Quest.REGICIDE.getId() ? 2 : 1;
			return null;
		}).when(client).runScript(any(Object[].class));
		when(client.getIntStack()).thenReturn(stack);

		AccountState state = AccountStateReader.read(client);

		assertEquals(93, (int) state.getSkillLevels().get("SLAYER"));
		assertEquals(0, (int) state.getSkillLevels().get("ATTACK"));
		assertEquals(Collections.singleton("DIARY_DESERT_HARD"), state.getCompletedDiaries());
		assertEquals(Collections.singleton("REGICIDE"), state.getCompletedQuests());
	}
}
