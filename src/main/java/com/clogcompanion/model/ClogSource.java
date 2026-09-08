package com.clogcompanion.model;

import lombok.Value;

/**
 * One collection log entry page (Zulrah, Master Treasure Trails, ...). Curated in
 * {@code clog-sources.json}; the time fields are what make estimates realistic.
 */
@Value
public class ClogSource
{
	String id;
	String name;
	Category category;
	/** Wall-clock minutes for one roll of the drop table, including everything needed to get that roll. */
	Double minutesPerAttempt;
	/** One-time cost before the first attempt. */
	Double setupMinutes;
	Requirements requirements;
	String notes;

	public boolean isRated()
	{
		return minutesPerAttempt != null && setupMinutes != null;
	}

	public Requirements getRequirements()
	{
		return requirements == null ? Requirements.none() : requirements;
	}
}
