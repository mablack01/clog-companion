package com.clogcompanion.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.clogcompanion.data.ClogDataset;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ObtainedTrackerTest
{
	private static final ClogDataset DATA = ClogDataset.load(new Gson());
	private static final Type SET = new TypeToken<Set<Integer>>()
	{
	}.getType();

	@Test
	public void chatMessageMarksEverySlotSharingTheName()
	{
		ConfigManager cm = mock(ConfigManager.class);
		ObtainedTracker t = new ObtainedTracker(DATA, cm, new Gson());
		assertFalse(t.onChatMessage("Your collection log has already accounted for that item."));
		assertFalse("unknown item: nothing to persist", t.onChatMessage("New item added to your collection log: Not an item"));
		assertTrue(t.onChatMessage("New item added to your collection log: Dragon pickaxe"));
		Set<String> slots = t.obtainedSlotIds();
		assertTrue(slots.contains("king_black_dragon:dragon_pickaxe"));
		assertTrue(slots.contains("kalphite_queen:dragon_pickaxe"));
		assertTrue(slots.size() >= 3);
		verify(cm).setRSProfileConfiguration(eq("clog-companion"), eq("obtainedItemIds"), any());
	}

	@Test
	public void scriptMarksAreSyncedAndPersistedOnceOnFlush()
	{
		ConfigManager cm = mock(ConfigManager.class);
		ObtainedTracker t = new ObtainedTracker(DATA, cm, new Gson());
		assertFalse(t.isSynced());
		t.markItemId(12921);
		t.markItemId(4708);
		t.markItemId(4708);
		assertTrue(t.isSynced());
		assertTrue(t.flush());
		assertFalse("nothing new to write", t.flush());
		ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
		verify(cm, times(1)).setRSProfileConfiguration(eq("clog-companion"), eq("obtainedItemIds"), json.capture());
		assertTrue(json.getValue().contains("12921") && json.getValue().contains("4708"));
		assertTrue(t.obtainedSlotIds().contains("zulrah:pet_snakeling"));
		assertTrue(t.obtainedSlotIds().contains("barrows_chests:ahrims_hood"));
	}

	@Test
	public void nameMarkingCoversVariantIds()
	{
		ConfigManager cm = mock(ConfigManager.class);
		ObtainedTracker t = new ObtainedTracker(DATA, cm, new Gson());
		t.markItemId(999999);
		t.markItemName("Plain satchel");
		assertTrue(t.isSynced());
		assertTrue(t.obtainedSlotIds().contains("creature_creation:plain_satchel"));
		t.markItemName("Not a collection log item");
		assertTrue(t.flush());
	}

	@Test
	public void loadRestoresFromProfileConfig()
	{
		ConfigManager cm = mock(ConfigManager.class);
		when(cm.getRSProfileConfiguration(eq("clog-companion"), eq("obtainedItemIds"), eq(SET))).thenReturn(new HashSet<>(Arrays.asList(12921)));
		ObtainedTracker t = new ObtainedTracker(DATA, cm, new Gson());
		t.load();
		assertTrue(t.isSynced());
		assertEquals(new HashSet<>(Arrays.asList(12921)), t.itemIds());
		t.clear();
		assertFalse(t.isSynced());
		assertTrue(t.obtainedSlotIds().isEmpty());
	}
}
