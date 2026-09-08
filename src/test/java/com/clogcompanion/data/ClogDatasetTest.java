package com.clogcompanion.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.clogcompanion.model.ClogItem;
import com.clogcompanion.model.ClogSource;
import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;

/** Integrity of the bundled data. Runs against the real resources. */
public class ClogDatasetTest
{
	private static final ClogDataset DATA = ClogDataset.load(new Gson());

	@Test
	public void loadsAFullLog()
	{
		assertTrue("expected >1000 slots, got " + DATA.getItems().size(), DATA.getItems().size() > 1000);
		assertTrue(DATA.getSources().size() >= 100);
	}

	@Test
	public void everyReferenceResolves()
	{
		List<String> problems = DATA.validate();
		assertTrue(String.join("\n", problems), problems.isEmpty());
	}

	@Test
	public void everyItemHasAnId()
	{
		long missing = DATA.getItems().stream().filter(i -> i.getItemId() <= 0).count();
		assertTrue("slots without an item id: " + missing, missing <= 5);
	}

	@Test
	public void reportsUnratedCoverage()
	{
		long unrated = DATA.getItems().stream()
			.filter(i -> i.getRate() == null || !DATA.timeSourceOf(i).isRated()).count();
		System.out.println("UNRATED slots: " + unrated + " / " + DATA.getItems().size());
		assertTrue("more than half the log is unrated", unrated * 2 < DATA.getItems().size());
	}

	@Test
	public void lookupsWork()
	{
		ClogItem whip = DATA.byId("abyssal_sire:abyssal_whip").orElseThrow(AssertionError::new);
		assertEquals("Abyssal Sire", DATA.sourceOf(whip).getName());
		assertTrue(DATA.byName("Dragon pickaxe").size() > 1);
	}

	@Test
	public void catchAllEntriesBorrowTheRealSourcesTime()
	{
		ClogItem pet = DATA.byId("all_pets:baby_mole").orElseThrow(AssertionError::new);
		ClogSource time = DATA.timeSourceOf(pet);
		assertEquals("giant_mole", time.getId());
		assertTrue(time.isRated());
	}
}
