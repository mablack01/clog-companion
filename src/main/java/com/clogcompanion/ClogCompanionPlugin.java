package com.clogcompanion;

import com.clogcompanion.data.ClogDataset;
import com.clogcompanion.engine.DifficultyEngine;
import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;
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

	@Inject
	private Gson gson;

	@Getter
	private ClogDataset dataset;

	@Override
	protected void startUp()
	{
		dataset = ClogDataset.load(gson);
		log.debug("Clog Companion loaded {} slots across {} entries", dataset.getItems().size(), dataset.getSources().size());
	}

	@Override
	protected void shutDown()
	{
		dataset = null;
	}

	public DifficultyEngine engine()
	{
		return new DifficultyEngine(config.estimateMode(), config.easyMaxMinutes(), config.mediumMaxMinutes(), config.longMaxMinutes());
	}

	@Provides
	ClogCompanionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClogCompanionConfig.class);
	}
}
