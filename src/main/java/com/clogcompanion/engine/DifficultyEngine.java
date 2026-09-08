package com.clogcompanion.engine;

import com.clogcompanion.model.ClogItem;
import com.clogcompanion.model.ClogSource;
import com.clogcompanion.model.Tier;
import java.util.OptionalDouble;

/** Turns a slot's drop rate and its source's per-attempt cost into minutes and a tier. */
public class DifficultyEngine
{
	private static final double LIKELY_PROBABILITY = 0.5;

	private final EstimateMode mode;
	private final int easyMax;
	private final int mediumMax;
	private final int longMax;

	public DifficultyEngine(EstimateMode mode, int easyMax, int mediumMax, int longMax)
	{
		this.mode = mode;
		this.easyMax = easyMax;
		this.mediumMax = mediumMax;
		this.longMax = longMax;
	}

	public OptionalDouble expectedMinutes(ClogItem item, ClogSource source)
	{
		Double rate = item.getRate();
		if (rate == null || rate <= 0 || source == null || !source.isRated())
		{
			return OptionalDouble.empty();
		}
		return OptionalDouble.of(source.getSetupMinutes() + attempts(rate) * source.getMinutesPerAttempt());
	}

	public Tier tier(ClogItem item, ClogSource source)
	{
		OptionalDouble minutes = expectedMinutes(item, source);
		if (!minutes.isPresent())
		{
			return Tier.UNRATED;
		}
		double m = minutes.getAsDouble();
		if (m <= easyMax)
		{
			return Tier.EASY;
		}
		if (m <= mediumMax)
		{
			return Tier.MEDIUM;
		}
		if (m <= longMax)
		{
			return Tier.LONG;
		}
		return Tier.GRIND;
	}

	private double attempts(double rate)
	{
		if (rate >= 1.0)
		{
			return 1;
		}
		if (mode == EstimateMode.LIKELY)
		{
			return Math.max(1, Math.log(1 - LIKELY_PROBABILITY) / Math.log1p(-rate));
		}
		return 1 / rate;
	}

	public static String formatMinutes(OptionalDouble minutes)
	{
		return minutes.isPresent() ? formatMinutes(minutes.getAsDouble()) : "?";
	}

	public static String formatMinutes(double minutes)
	{
		long total = Math.round(minutes);
		long days = total / 1440;
		long hours = (total % 1440) / 60;
		long mins = total % 60;
		if (days > 0)
		{
			return days + "d " + hours + "h";
		}
		if (hours > 0)
		{
			return hours + "h " + mins + "m";
		}
		return mins + "m";
	}
}
