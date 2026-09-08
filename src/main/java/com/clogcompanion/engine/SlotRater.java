package com.clogcompanion.engine;

import com.clogcompanion.data.ClogDataset;
import com.clogcompanion.model.AccountState;
import com.clogcompanion.model.ClogItem;
import com.clogcompanion.model.ClogSource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The one place a slot is turned into a {@link RatedSlot}: it picks the time source
 * (which differs from the listing entry for catch-all entries) so callers cannot get it wrong.
 */
public class SlotRater
{
	private final ClogDataset dataset;
	private final DifficultyEngine engine;

	public SlotRater(ClogDataset dataset, DifficultyEngine engine)
	{
		this.dataset = dataset;
		this.engine = engine;
	}

	public RatedSlot rate(ClogItem item, AccountState state, Set<String> obtainedIds)
	{
		ClogSource entry = dataset.sourceOf(item);
		ClogSource time = dataset.timeSourceOf(item);
		return new RatedSlot(item, entry, engine.tier(item, time), DifficultyEngine.challenge(item, time), engine.expectedMinutes(item, time),
			RequirementChecker.unmet(time.getRequirements(), state), obtainedIds.contains(item.getId()));
	}

	public List<RatedSlot> rateAll(AccountState state, Set<String> obtainedIds)
	{
		return dataset.getItems().stream().map(i -> rate(i, state, obtainedIds)).collect(Collectors.toList());
	}
}
