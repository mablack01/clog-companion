package com.clogcompanion.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.clogcompanion.model.AccountState;
import com.clogcompanion.model.Requirements;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class RequirementCheckerTest
{
	private static final Requirements REQ = new Requirements(
		ImmutableMap.of("SLAYER", 93),
		Collections.singletonList("REGICIDE"),
		Collections.singletonList("DIARY_DESERT_HARD"));

	@Test
	public void allMet()
	{
		AccountState state = new AccountState(ImmutableMap.of("SLAYER", 95),
			ImmutableSet.of("REGICIDE"), ImmutableSet.of("DIARY_DESERT_HARD"));
		assertTrue(RequirementChecker.unmet(REQ, state).isEmpty());
	}

	@Test
	public void reportsEveryUnmetReason()
	{
		AccountState state = new AccountState(ImmutableMap.of("SLAYER", 87), ImmutableSet.of(), ImmutableSet.of());
		assertEquals(
			Arrays.asList("Slayer 93 (have 87)", "Regicide not completed", "Desert Hard diary not completed"),
			RequirementChecker.unmet(REQ, state));
	}

	@Test
	public void missingSkillCountsAsLevelOne()
	{
		assertEquals("Slayer 93 (have 1)", RequirementChecker.unmet(REQ, AccountState.empty()).get(0));
	}

	@Test
	public void noRequirementsIsAlwaysMet()
	{
		assertTrue(RequirementChecker.unmet(Requirements.none(), AccountState.empty()).isEmpty());
	}

	@Test
	public void titleCasesMultiWordConstants()
	{
		assertEquals("Desert Treasure II The Fallen Empire", RequirementChecker.titleCase("DESERT_TREASURE_II__THE_FALLEN_EMPIRE"));
	}
}
