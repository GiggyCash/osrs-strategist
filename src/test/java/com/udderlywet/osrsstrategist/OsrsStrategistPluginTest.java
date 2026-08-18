package com.udderlywet.osrsstrategist;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OsrsStrategistPluginTest
{
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception
    {
        // RuneLite's varargs signature uses Class<? extends Plugin> and emits
        // an unavoidable generic-array warning at this development launcher.
        ExternalPluginManager.loadBuiltin(OsrsStrategistPlugin.class);
        RuneLite.main(args);
    }
}
