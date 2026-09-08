package com.clogcompanion;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ClogCompanionPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ClogCompanionPlugin.class);
		RuneLite.main(args);
	}
}
