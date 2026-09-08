package com.clogcompanion.engine;

public enum EstimateMode
{
	/** Mean attempts: 1 / rate. */
	EXPECTED("Expected (average)"),
	/** Median attempts: the number by which half of players have the drop. */
	LIKELY("Likely (median)");

	private final String label;

	EstimateMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
