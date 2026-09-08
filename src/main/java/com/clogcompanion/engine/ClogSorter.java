package com.clogcompanion.engine;

import java.util.Comparator;

public enum ClogSorter implements Comparator<RatedSlot>
{
	FASTEST("Fastest first")
	{
		@Override
		public int compare(RatedSlot a, RatedSlot b)
		{
			return Double.compare(a.getMinutes().orElse(Double.MAX_VALUE), b.getMinutes().orElse(Double.MAX_VALUE));
		}
	},
	RAREST("Rarest first")
	{
		@Override
		public int compare(RatedSlot a, RatedSlot b)
		{
			return Double.compare(rate(a), rate(b));
		}
	},
	NAME("A-Z")
	{
		@Override
		public int compare(RatedSlot a, RatedSlot b)
		{
			return String.CASE_INSENSITIVE_ORDER.compare(a.getItem().getName(), b.getItem().getName());
		}
	};

	private final String label;

	ClogSorter(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}

	private static double rate(RatedSlot s)
	{
		Double r = s.getItem().getRate();
		return r == null ? Double.MAX_VALUE : r;
	}
}
