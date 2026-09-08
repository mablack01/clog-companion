package com.clogcompanion;

import com.clogcompanion.engine.EstimateMode;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Units;

@ConfigGroup(ClogCompanionConfig.GROUP)
public interface ClogCompanionConfig extends Config
{
	String GROUP = "clog-companion";

	@ConfigItem(
		keyName = "estimateMode",
		name = "Estimate",
		description = "Expected: average attempts (1 / rate). Likely: attempts by which half of players have the drop.",
		position = 0
	)
	default EstimateMode estimateMode()
	{
		return EstimateMode.EXPECTED;
	}

	@ConfigItem(
		keyName = "easyMaxMinutes",
		name = "Easy up to",
		description = "Slots estimated at or under this many minutes are Easy",
		position = 1
	)
	@Units(Units.MINUTES)
	default int easyMaxMinutes()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "mediumMaxMinutes",
		name = "Medium up to",
		description = "Slots estimated at or under this many minutes are Medium",
		position = 2
	)
	@Units(Units.MINUTES)
	default int mediumMaxMinutes()
	{
		return 300;
	}

	@ConfigItem(
		keyName = "longMaxMinutes",
		name = "Long up to",
		description = "Slots estimated at or under this many minutes are Long; anything above is a Grind",
		position = 3
	)
	@Units(Units.MINUTES)
	default int longMaxMinutes()
	{
		return 1500;
	}
}
