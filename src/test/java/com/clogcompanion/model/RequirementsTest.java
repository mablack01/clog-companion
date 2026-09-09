package com.clogcompanion.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RequirementsTest
{
	@Test
	public void nullCollectionsReadAsEmpty()
	{
		Requirements r = new Requirements(null, null, null);
		assertTrue(r.getSkills().isEmpty());
		assertTrue(r.getQuests().isEmpty());
		assertTrue(r.getDiaries().isEmpty());
	}

	@Test
	public void sourceWithoutTimesIsUnrated()
	{
		ClogSource unrated = new ClogSource("x", "X", Category.OTHER, null, 0.0, Requirements.none(), null, null);
		assertFalse(unrated.isRated());
		ClogSource rated = new ClogSource("x", "X", Category.OTHER, 5.0, 0.0, Requirements.none(), null, null);
		assertTrue(rated.isRated());
	}

	@Test
	public void nullRequirementsReadAsNone()
	{
		ClogSource s = new ClogSource("x", "X", Category.OTHER, 5.0, 0.0, null, null, null);
		assertTrue(s.getRequirements().getSkills().isEmpty());
	}
}
