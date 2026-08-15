package com.udderlywet.osrsstrategist;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OsrsStrategistConfig.GROUP)
public interface OsrsStrategistConfig extends Config
{
    String GROUP = "osrs-strategist";

    @ConfigItem(keyName = "strategyMode", name = "Strategy style", description = "How the strategist weighs speed versus comfort")
    default StrategyMode strategyMode() { return StrategyMode.BALANCED; }

    @ConfigItem(keyName = "questTolerance", name = "Quest tolerance", description = "How often quests should appear in recommendations")
    default QuestTolerance questTolerance() { return QuestTolerance.NORMAL; }

    @ConfigItem(keyName = "useGroupStorage", name = "Use Group Storage", description = "For GIM accounts, count useful items already present in Group Storage")
    default boolean useGroupStorage() { return true; }

    @ConfigItem(keyName = "bankAware", name = "Bank-aware strategy", description = "Use the most recent bank snapshot when weighing options")
    default boolean bankAware() { return true; }

    @ConfigItem(keyName = "birdhouseReminders", name = "Birdhouse reminders", description = "Remind when a birdhouse run is ready and show a preparation checklist")
    default boolean birdhouseReminders() { return true; }

    @ConfigItem(keyName = "herbRunReminders", name = "Herb run reminders", description = "Remind when an herb run is ready and show a preparation checklist")
    default boolean herbRunReminders() { return true; }

    @ConfigItem(keyName = "clueReminders", name = "Clue reminders", description = "Surface clues at good times without constantly nagging")
    default boolean clueReminders() { return true; }
}
