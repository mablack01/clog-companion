package com.clogcompanion.model;

import java.util.Locale;

public enum Category
{
	BOSSES,
	RAIDS,
	CLUES,
	MINIGAMES,
	OTHER;

	@Override
	public String toString()
	{
		return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
	}
}
