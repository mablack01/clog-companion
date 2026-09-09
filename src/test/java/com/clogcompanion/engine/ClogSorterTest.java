package com.clogcompanion.engine;

import static org.junit.Assert.assertEquals;
import com.clogcompanion.model.Category;
import com.clogcompanion.model.ClogItem;
import com.clogcompanion.model.ClogSource;
import com.clogcompanion.model.Requirements;
import com.clogcompanion.model.Tier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import org.junit.Test;

public class ClogSorterTest
{
	private static RatedSlot slot(String name, Double rate, OptionalDouble minutes)
	{
		ClogSource src = new ClogSource("s", "Src", Category.BOSSES, 1.0, 0.0, Requirements.none(), null, null);
		ClogItem it = new ClogItem("s:" + name, "s", null, 1, name, rate, "", null, null);
		return new RatedSlot(it, src, minutes.isPresent() ? Tier.EASY : Tier.UNRATED, 1, minutes, Collections.emptyList(), false);
	}

	private static final RatedSlot B = slot("B", 0.5, OptionalDouble.of(20));
	private static final RatedSlot A = slot("a", 0.1, OptionalDouble.of(5));
	private static final RatedSlot C = slot("c", null, OptionalDouble.empty());

	private static List<String> order(ClogSorter s)
	{
		return Arrays.asList(C, B, A).stream().sorted(s).map(r -> r.getItem().getName()).collect(Collectors.toList());
	}

	@Test
	public void fastestPutsUnratedLast()
	{
		assertEquals(Arrays.asList("a", "B", "c"), order(ClogSorter.FASTEST));
	}

	@Test
	public void rarestIsAscendingRateUnknownLast()
	{
		assertEquals(Arrays.asList("a", "B", "c"), order(ClogSorter.RAREST));
	}

	@Test
	public void easiestOrdersByTierThenTime()
	{
		RatedSlot hardButQuick = slot("d", 0.9, OptionalDouble.of(1));
		hardButQuick = new RatedSlot(hardButQuick.getItem(), hardButQuick.getSource(), Tier.LONG, 4, OptionalDouble.of(1), Collections.emptyList(), false);
		List<String> order = Arrays.asList(C, hardButQuick, B, A).stream().sorted(ClogSorter.EASIEST)
			.map(r -> r.getItem().getName()).collect(Collectors.toList());
		assertEquals(Arrays.asList("a", "B", "d", "c"), order);
	}

	@Test
	public void nameIsCaseInsensitiveAlphabetical()
	{
		assertEquals(Arrays.asList("a", "B", "c"), order(ClogSorter.NAME));
	}
}
