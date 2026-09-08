package com.clogcompanion.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Timing and choice for the roll: decided up front so the animation is purely cosmetic. */
final class RollAnimation
{
	private static final int START_MS = 40;
	private static final int END_MS = 400;
	private static final double GROWTH = 1.12;

	private RollAnimation()
	{
	}

	/** Delays between frames, fast to slow; the last frame shows the winner. */
	static List<Integer> delays()
	{
		List<Integer> out = new ArrayList<>();
		double d = START_MS;
		while (d < END_MS)
		{
			out.add((int) Math.round(d));
			d *= GROWTH;
		}
		out.add(END_MS);
		return out;
	}

	static int pick(Random random, int size)
	{
		return random.nextInt(size);
	}

	/** A frame index different from the previous one, so the spin visibly moves. */
	static int next(Random random, int size, int previous)
	{
		if (size < 2)
		{
			return 0;
		}
		int n = random.nextInt(size - 1);
		return n >= previous ? n + 1 : n;
	}
}
