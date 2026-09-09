package com.clogcompanion.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TrackedListTest
{
	private static final Type SET = new TypeToken<Set<String>>()
	{
	}.getType();

	@Test
	public void toggleAddsThenRemovesAndPersistsEachTime()
	{
		ConfigManager cm = mock(ConfigManager.class);
		TrackedList t = new TrackedList(cm, new Gson());
		assertTrue(t.toggle("zulrah:tanzanite_fang"));
		assertTrue(t.contains("zulrah:tanzanite_fang"));
		assertFalse(t.toggle("zulrah:tanzanite_fang"));
		assertEquals(0, t.size());
		ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
		verify(cm, times(2)).setRSProfileConfiguration(eq("clog-companion"), eq("trackedSlots"), json.capture());
		assertEquals("[\"zulrah:tanzanite_fang\"]", json.getAllValues().get(0));
		assertEquals("[]", json.getAllValues().get(1));
	}

	@Test
	public void clearCompletedDropsOnlyOwnedSlots()
	{
		ConfigManager cm = mock(ConfigManager.class);
		TrackedList t = new TrackedList(cm, new Gson());
		t.add("a:x");
		t.add("a:y");
		assertEquals(1, t.clearCompleted(new HashSet<>(Arrays.asList("a:y", "b:z"))));
		assertEquals(Collections.singleton("a:x"), t.ids());
		assertEquals(0, t.clearCompleted(Collections.emptySet()));
	}

	@Test
	public void loadRestoresFromProfileAndClearForgets()
	{
		ConfigManager cm = mock(ConfigManager.class);
		when(cm.getRSProfileConfiguration(eq("clog-companion"), eq("trackedSlots"), eq(SET))).thenReturn(new HashSet<>(Collections.singletonList("a:x")));
		TrackedList t = new TrackedList(cm, new Gson());
		t.load();
		assertTrue(t.contains("a:x"));
		t.clear();
		assertEquals(0, t.size());
	}
}
