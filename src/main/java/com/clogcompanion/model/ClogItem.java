package com.clogcompanion.model;

import lombok.Value;

/**
 * One collection log slot: an item under a specific entry. Generated into
 * {@code clog-items.json} from the OSRS Wiki.
 */
@Value
public class ClogItem
{
	String id;
	String sourceId;
	int itemId;
	String name;
	/** Probability per attempt of the source; 1.0 = guaranteed, null = unknown. */
	Double rate;
	String rateText;
	String wikiUrl;
}
