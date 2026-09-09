package com.clogcompanion.model;

import java.util.Locale;

public enum Tier
{
	EASY,
	MEDIUM,
	LONG,
	GRIND,
	UNRATED;

	@Override
	public String toString()
	{
		return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
	}
}
