package com.clogcompanion.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.clogcompanion.model.Category;
import com.clogcompanion.model.ClogItem;
import com.clogcompanion.model.ClogSource;
import com.clogcompanion.model.Requirements;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Proves {@link ClogDataset#validate()} actually catches each class of data defect. */
public class ClogDatasetValidateTest
{
	private static ClogSource source(String id, Category cat, Requirements req)
	{
		return new ClogSource(id, id, cat, 1.0, 0.0, req, null, null);
	}

	private static ClogItem item(String id, String sourceId, String timeSourceId)
	{
		return new ClogItem(id, sourceId, timeSourceId, 1, id, 0.5, "1/2", null, null);
	}

	@Test
	public void cleanDataHasNoProblems()
	{
		ClogDataset d = new ClogDataset(Collections.singletonList(item("a:x", "a", null)),
			Collections.singletonList(source("a", Category.BOSSES, Requirements.none())));
		assertTrue(d.validate().isEmpty());
	}

	@Test
	public void reportsEveryDefect()
	{
		Requirements bad = new Requirements(ImmutableMap.of("SLAYING", 1), Collections.singletonList("NOT_A_QUEST"),
			Collections.singletonList("DIARY_NOWHERE_EASY"));
		ClogDataset d = new ClogDataset(
			Arrays.asList(item("a:x", "a", null), item("a:x", "a", null), item("a:y", "missing", "gone")),
			Arrays.asList(source("a", null, bad)));
		List<String> problems = d.validate();
		assertEquals(String.join("\n", problems), 7, problems.size());
		assertTrue(problems.contains("a:x: duplicate id"));
		assertTrue(problems.contains("a:y: unknown sourceId missing"));
		assertTrue(problems.contains("a:y: unknown timeSourceId gone"));
		assertTrue(problems.contains("a: unknown category"));
		assertTrue(problems.contains("a: unknown skill SLAYING"));
		assertTrue(problems.contains("a: unknown quest NOT_A_QUEST"));
		assertTrue(problems.contains("a: unknown diary DIARY_NOWHERE_EASY"));
	}
}
