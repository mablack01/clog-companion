package com.clogcompanion;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Clog Companion",
	description = "Ranks Collection Log slots by how quick they are to get",
	tags = {"collection log", "clog", "completionist", "drop rate"}
)
public class ClogCompanionPlugin extends Plugin
{
	@Inject
	private ClogCompanionConfig config;

	@Override
	protected void startUp()
	{
		log.debug("Clog Companion started");
	}

	@Override
	protected void shutDown()
	{
		log.debug("Clog Companion stopped");
	}

	@Provides
	ClogCompanionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClogCompanionConfig.class);
	}
}
