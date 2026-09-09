package com.clogcompanion.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.clogcompanion.data.ClogDataset;
import com.clogcompanion.model.AccountState;
import com.clogcompanion.model.ClogItem;
import com.clogcompanion.model.Tier;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class SlotRaterTest
{
	private static final ClogDataset DATA = ClogDataset.load(new Gson());
	private static final SlotRater RATER = new SlotRater(DATA, new DifficultyEngine(EstimateMode.EXPECTED, 60, 300, 1500));

	@Test
	public void catchAllSlotIsRatedThroughItsTimeSource()
	{
		ClogItem pet = DATA.byId("all_pets:baby_mole").orElseThrow(AssertionError::new);
		RatedSlot slot = RATER.rate(pet, AccountState.empty(), Collections.emptySet());
		assertEquals("All Pets", slot.getSource().getName());
		assertTrue(slot.getMinutes().isPresent());
		assertEquals(Tier.GRIND, slot.getTier());
		assertFalse(slot.isObtained());
	}

	@Test
	public void obtainedAndRequirementsComeThrough()
	{
		ClogItem fang = DATA.byId("zulrah:tanzanite_fang").orElseThrow(AssertionError::new);
		RatedSlot slot = RATER.rate(fang, AccountState.empty(), Collections.singleton(fang.getId()));
		assertTrue(slot.isObtained());
		assertEquals(Collections.singletonList("Regicide not completed"), slot.getUnmet());
	}

	@Test
	public void curatedChallengeSurvivesRegeneration()
	{
		assertEquals(Tier.GRIND, tierOf("the_inferno:infernal_cape"));
		assertEquals(Tier.LONG, tierOf("phantom_muspah:charged_ice"));
		assertEquals(Tier.GRIND, tierOf("araxxor:coagulated_venom"));
		assertEquals(Tier.GRIND, tierOf("tombs_of_amascut:cursed_phalanx"));
		assertEquals(Tier.EASY, tierOf("giant_mole:mole_claw"));
		assertEquals(Tier.MEDIUM, tierOf("dagannoth_kings:berserker_ring"));
	}

	private static Tier tierOf(String id)
	{
		ClogItem item = DATA.byId(id).orElseThrow(AssertionError::new);
		return RATER.rate(item, AccountState.empty(), Collections.emptySet()).getTier();
	}

	@Test
	public void fastestRatedSlotsLookSane()
	{
		List<RatedSlot> fastest = RATER.rateAll(AccountState.empty(), Collections.emptySet()).stream()
			.filter(s -> s.getMinutes().isPresent()).sorted(ClogSorter.FASTEST).limit(20)
			.collect(java.util.stream.Collectors.toList());
		fastest.forEach(s -> System.out.println("FASTEST " + s.getItem().getId() + " " + DifficultyEngine.formatMinutes(s.getMinutes().getAsDouble())));
		assertEquals(20, fastest.size());
		assertTrue(fastest.get(19).getMinutes().getAsDouble() <= 60);
	}
}
