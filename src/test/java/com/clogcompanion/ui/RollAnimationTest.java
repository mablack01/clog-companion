package com.clogcompanion.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class RollAnimationTest
{
	@Test
	public void delaysSlowDownOverAboutThreeSeconds()
	{
		List<Integer> d = RollAnimation.delays();
		assertTrue(d.get(0) <= 50);
		assertTrue("enough frames to read as a spin", d.size() >= 10);
		assertEquals(400, (int) d.get(d.size() - 1));
		for (int i = 1; i < d.size(); i++)
		{
			assertTrue(d.get(i) >= d.get(i - 1));
		}
		int total = d.stream().mapToInt(Integer::intValue).sum();
		assertTrue("total " + total, total >= 2500 && total <= 4000);
	}

	@Test
	public void pickAndNextStayInBounds()
	{
		Random r = new Random(7);
		for (int i = 0; i < 200; i++)
		{
			int p = RollAnimation.pick(r, 5);
			assertTrue(p >= 0 && p < 5);
			int n = RollAnimation.next(r, 5, p);
			assertTrue(n >= 0 && n < 5);
			assertNotEquals(p, n);
		}
		assertEquals(0, RollAnimation.next(r, 1, 0));
	}
}
