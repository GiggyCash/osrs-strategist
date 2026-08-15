package com.udderlywet.osrsstrategist;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OsrsStrategistPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(OsrsStrategistPlugin.class);
        RuneLite.main(args);
    }
}
