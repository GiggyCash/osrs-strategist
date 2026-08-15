package com.udderlywet.osrsstrategist;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
    name = "OSRS Strategist",
    description = "Adaptive progression strategy for every OSRS account type",
    tags = {"strategy", "progression", "maxing", "ironman", "uim", "gim", "clues", "farming"}
)
public class OsrsStrategistPlugin extends Plugin
{
    @Inject
    private OsrsStrategistConfig config;

    @Provides
    OsrsStrategistConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(OsrsStrategistConfig.class);
    }

    @Override
    protected void startUp()
    {
        // v0.2 foundation: intentionally small entry point.
        // Account readers, scoring, opportunity reminders, and the UI are added
        // as separate services so new game content does not turn this class into
        // a giant fragile plugin.
    }

    @Override
    protected void shutDown()
    {
    }
}
