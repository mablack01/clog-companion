package com.clogcompanion.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.clogcompanion.model.Category;
import com.clogcompanion.model.ClogItem;
import com.clogcompanion.model.ClogSource;
import com.clogcompanion.model.Requirements;
import com.clogcompanion.model.Tier;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import org.junit.Test;

public class ClogFilterTest
{
	private static RatedSlot slot(String name, Category cat, Tier tier, double minutes, boolean obtained, List<String> unmet)
	{
		ClogSource src = new ClogSource("s", "Src", cat, 1, 0, Requirements.none(), null);
		ClogItem it = new ClogItem("s:" + name, "s", 1, name, 0.5, "1/2", null);
		return new RatedSlot(it, src, tier, OptionalDouble.of(minutes), unmet, obtained);
	}

	private static final RatedSlot EASY_BOSS = slot("Whip", Category.BOSSES, Tier.EASY, 10, false, Collections.emptyList());
	private static final RatedSlot GRIND_CLUE = slot("3rd age", Category.CLUES, Tier.GRIND, 9999, false, Collections.emptyList());
	private static final RatedSlot OWNED = slot("Pet", Category.BOSSES, Tier.EASY, 10, true, Collections.emptyList());
	private static final RatedSlot LOCKED = slot("Fang", Category.RAIDS, Tier.MEDIUM, 100, false, Collections.singletonList("Slayer 93 (have 1)"));
	private static final List<RatedSlot> ALL = Arrays.asList(EASY_BOSS, GRIND_CLUE, OWNED, LOCKED);

	private static List<String> names(ClogFilter f)
	{
		return ALL.stream().filter(f::test).map(s -> s.getItem().getName()).collect(Collectors.toList());
	}

	@Test
	public void defaultFilterHidesObtainedOnly()
	{
		assertEquals(Arrays.asList("Whip", "3rd age", "Fang"), names(new ClogFilter()));
	}

	@Test
	public void tierAndCategory()
	{
		ClogFilter f = new ClogFilter();
		f.setTiers(EnumSet.of(Tier.EASY));
		f.setHideObtained(false);
		assertEquals(Arrays.asList("Whip", "Pet"), names(f));
		f.setCategory(Category.CLUES);
		assertTrue(names(f).isEmpty());
	}

	@Test
	public void requirementsAndSearch()
	{
		ClogFilter f = new ClogFilter();
		f.setOnlyMeetsRequirements(true);
		assertEquals(Arrays.asList("Whip", "3rd age"), names(f));
		f.setSearch("AGE");
		assertEquals(Collections.singletonList("3rd age"), names(f));
	}

	@Test
	public void searchMatchesSourceNameToo()
	{
		ClogFilter f = new ClogFilter();
		f.setSearch("  src ");
		assertEquals(3, names(f).size());
	}
}
