package com.clogcompanion.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import com.clogcompanion.model.Category;
import com.clogcompanion.model.ClogItem;
import com.clogcompanion.model.ClogSource;
import com.clogcompanion.model.Requirements;
import com.clogcompanion.model.Tier;
import org.junit.Test;

public class DifficultyEngineTest
{
	private static final DifficultyEngine EXPECTED = new DifficultyEngine(EstimateMode.EXPECTED, 60, 300, 1500);
	private static final DifficultyEngine LIKELY = new DifficultyEngine(EstimateMode.LIKELY, 60, 300, 1500);

	private static ClogSource source(Double perAttempt, Double setup)
	{
		return new ClogSource("s", "S", Category.BOSSES, perAttempt, setup, Requirements.none(), null, null);
	}

	private static ClogItem item(Double rate)
	{
		return new ClogItem("s:i", "s", null, 1, "I", rate, "1/x", null, null);
	}

	@Test
	public void expectedModeIsOneOverRate()
	{
		assertEquals(2.0 * 512, EXPECTED.expectedMinutes(item(1.0 / 512), source(2.0, 0.0)).getAsDouble(), 1e-6);
	}

	@Test
	public void likelyModeUsesMedianAttempts()
	{
		// ln(0.5) / ln(1 - 1/512) ≈ 354.5 attempts
		assertEquals(354.5, LIKELY.expectedMinutes(item(1.0 / 512), source(1.0, 0.0)).getAsDouble(), 0.5);
	}

	@Test
	public void likelyModeNeverEstimatesUnderOneAttempt()
	{
		// ln(0.5)/ln(0.25) = 0.5 attempts; a drop still costs at least one attempt.
		assertEquals(40.0, LIKELY.expectedMinutes(item(0.75), source(40.0, 0.0)).getAsDouble(), 1e-9);
	}

	@Test
	public void setupIsAddedOnce()
	{
		assertEquals(100 + 10, EXPECTED.expectedMinutes(item(1.0), source(10.0, 100.0)).getAsDouble(), 1e-9);
	}

	@Test
	public void guaranteedDropIsOneAttemptInBothModes()
	{
		assertEquals(5.0, EXPECTED.expectedMinutes(item(1.0), source(5.0, 0.0)).getAsDouble(), 1e-9);
		assertEquals(5.0, LIKELY.expectedMinutes(item(1.0), source(5.0, 0.0)).getAsDouble(), 1e-9);
	}

	@Test
	public void missingRateOrTimeIsUnrated()
	{
		assertFalse(EXPECTED.expectedMinutes(item(null), source(5.0, 0.0)).isPresent());
		assertFalse(EXPECTED.expectedMinutes(item(0.5), source(null, 0.0)).isPresent());
		assertFalse(EXPECTED.expectedMinutes(item(0.5), null).isPresent());
		assertEquals(Tier.UNRATED, EXPECTED.tier(item(null), source(5.0, 0.0)));
	}

	@Test
	public void tierThresholdsAreInclusiveUpperBounds()
	{
		assertEquals(Tier.EASY, EXPECTED.tier(item(1.0), source(60.0, 0.0)));
		assertEquals(Tier.MEDIUM, EXPECTED.tier(item(1.0), source(61.0, 0.0)));
		assertEquals(Tier.MEDIUM, EXPECTED.tier(item(1.0), source(300.0, 0.0)));
		assertEquals(Tier.LONG, EXPECTED.tier(item(1.0), source(301.0, 0.0)));
		assertEquals(Tier.LONG, EXPECTED.tier(item(1.0), source(1500.0, 0.0)));
		assertEquals(Tier.GRIND, EXPECTED.tier(item(1.0), source(1501.0, 0.0)));
	}

	@Test
	public void clueCasketUniqueIsNotEasy()
	{
		// Opening the casket is instant, but each attempt is a whole master clue (~25 min).
		assertEquals(Tier.GRIND, EXPECTED.tier(item(1.0 / 28988), source(25.0, 0.0)));
		assertEquals(Tier.LONG, EXPECTED.tier(item(1.0 / 50), source(25.0, 0.0)));
		assertEquals(Tier.MEDIUM, EXPECTED.tier(item(1.0 / 5), source(25.0, 0.0)));
	}

	@Test
	public void challengeRaisesTheTierFloorButNeverLowersIt()
	{
		ClogSource easyBoss = new ClogSource("s", "S", Category.BOSSES, 1.5, 0.0, Requirements.none(), 4, null);
		assertEquals(Tier.LONG, EXPECTED.tier(item(1.0), easyBoss));
		ClogSource grind = new ClogSource("s", "S", Category.BOSSES, 2000.0, 0.0, Requirements.none(), 2, null);
		assertEquals(Tier.GRIND, EXPECTED.tier(item(1.0), grind));
		ClogItem conditional = new ClogItem("s:i", "s", null, 1, "I", 1.0, "Always", null, 5);
		assertEquals(Tier.GRIND, EXPECTED.tier(conditional, source(3.0, 0.0)));
		assertEquals(5, DifficultyEngine.challenge(conditional, easyBoss));
		assertEquals(Tier.UNRATED, EXPECTED.tier(item(null), easyBoss));
		assertEquals(1, DifficultyEngine.challenge(item(1.0), new ClogSource("s", "S", Category.BOSSES, 1.0, 0.0, null, 0, null)));
		assertEquals(5, DifficultyEngine.challenge(item(1.0), new ClogSource("s", "S", Category.BOSSES, 1.0, 0.0, null, 7, null)));
		assertEquals(1, DifficultyEngine.challenge(item(1.0), null));
	}

	@Test
	public void formatMinutes()
	{
		assertEquals("45m", DifficultyEngine.formatMinutes(45));
		assertEquals("2h 10m", DifficultyEngine.formatMinutes(130));
		assertEquals("3d 4h", DifficultyEngine.formatMinutes(3 * 1440 + 4 * 60 + 12));
		assertEquals("0m", DifficultyEngine.formatMinutes(0.2));
	}
}
