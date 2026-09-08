package com.clogcompanion.engine;

import com.clogcompanion.model.ClogItem;
import com.clogcompanion.model.ClogSource;
import com.clogcompanion.model.Tier;
import java.util.List;
import java.util.OptionalDouble;
import lombok.Value;

/** A slot joined with its source and evaluated for the current account; what the UI lists. */
@Value
public class RatedSlot
{
	ClogItem item;
	ClogSource source;
	Tier tier;
	OptionalDouble minutes;
	List<String> unmet;
	boolean obtained;
}
