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

	private static ClogSource source(Integer perAttempt, Integer setup)
	{
		return new ClogSource("s", "S", Category.BOSSES, perAttempt, setup, Requirements.none(), null);
	}

	private static ClogItem item(Double rate)
	{
		return new ClogItem("s:i", "s", 1, "I", rate, "1/x", null);
	}

	@Test
	public void expectedModeIsOneOverRate()
	{
		assertEquals(2.0 * 512, EXPECTED.expectedMinutes(item(1.0 / 512), source(2, 0)).getAsDouble(), 1e-6);
	}

	@Test
	public void likelyModeUsesMedianAttempts()
	{
		// ln(0.5) / ln(1 - 1/512) ≈ 354.5 attempts
		assertEquals(354.5, LIKELY.expectedMinutes(item(1.0 / 512), source(1, 0)).getAsDouble(), 0.5);
	}

	@Test
	public void setupIsAddedOnce()
	{
		assertEquals(100 + 10, EXPECTED.expectedMinutes(item(1.0), source(10, 100)).getAsDouble(), 1e-9);
	}

	@Test
	public void guaranteedDropIsOneAttemptInBothModes()
	{
		assertEquals(5.0, EXPECTED.expectedMinutes(item(1.0), source(5, 0)).getAsDouble(), 1e-9);
		assertEquals(5.0, LIKELY.expectedMinutes(item(1.0), source(5, 0)).getAsDouble(), 1e-9);
	}

	@Test
	public void missingRateOrTimeIsUnrated()
	{
		assertFalse(EXPECTED.expectedMinutes(item(null), source(5, 0)).isPresent());
		assertFalse(EXPECTED.expectedMinutes(item(0.5), source(null, 0)).isPresent());
		assertFalse(EXPECTED.expectedMinutes(item(0.5), null).isPresent());
		assertEquals(Tier.UNRATED, EXPECTED.tier(item(null), source(5, 0)));
	}

	@Test
	public void tierThresholdsAreInclusiveUpperBounds()
	{
		assertEquals(Tier.EASY, EXPECTED.tier(item(1.0), source(60, 0)));
		assertEquals(Tier.MEDIUM, EXPECTED.tier(item(1.0), source(61, 0)));
		assertEquals(Tier.MEDIUM, EXPECTED.tier(item(1.0), source(300, 0)));
		assertEquals(Tier.LONG, EXPECTED.tier(item(1.0), source(301, 0)));
		assertEquals(Tier.LONG, EXPECTED.tier(item(1.0), source(1500, 0)));
		assertEquals(Tier.GRIND, EXPECTED.tier(item(1.0), source(1501, 0)));
	}

	@Test
	public void clueCasketUniqueIsNotEasy()
	{
		// Opening the casket is instant, but each attempt is a whole master clue (~25 min).
		assertEquals(Tier.GRIND, EXPECTED.tier(item(1.0 / 28988), source(25, 0)));
		assertEquals(Tier.LONG, EXPECTED.tier(item(1.0 / 50), source(25, 0)));
		assertEquals(Tier.MEDIUM, EXPECTED.tier(item(1.0 / 5), source(25, 0)));
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
