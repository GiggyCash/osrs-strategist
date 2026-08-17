package com.udderlywet.osrsstrategist;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OsrsStrategistConfig.GROUP)
public interface OsrsStrategistConfig extends Config
{
    String GROUP = "osrs-strategist";

    @ConfigItem(keyName = "strategyMode", name = "Strategy style",
            description = "How the strategist weighs speed versus comfort")
    default StrategyMode strategyMode() { return StrategyMode.BALANCED; }

    @ConfigItem(keyName = "sessionIntent", name = "Session intent",
            description = "What kind of session you want Strategist to plan for")
    default SessionIntent sessionIntent() { return SessionIntent.PICK_FOR_ME; }

    @ConfigItem(keyName = "activeGoal", name = "Big goal",
            description = "The long-term goal Strategist should optimize around")
    default GoalType activeGoal() { return GoalType.MAX; }

    @ConfigItem(keyName = "questTolerance", name = "Quest tolerance",
            description = "How often quests should appear in recommendations")
    default QuestTolerance questTolerance() { return QuestTolerance.NORMAL; }

    @ConfigItem(keyName = "sidebarTextSize", name = "Sidebar text size",
            description = "Increase Strategist text size without changing the rest of RuneLite")
    default SidebarTextSize sidebarTextSize() { return SidebarTextSize.LARGE; }

    @ConfigItem(keyName = "allowWildernessMethods", name = "Wilderness methods",
            description = "Allow Strategist to recommend methods that require entering the Wilderness")
    default boolean allowWildernessMethods() { return false; }

    @ConfigItem(keyName = "showInGameGuidance", name = "In-game guidance",
            description = "Show the current Strategist checklist as a movable game-screen overlay")
    default boolean showInGameGuidance() { return true; }

    @ConfigItem(keyName = "useGroupStorage", name = "Use Group Storage",
            description = "For GIM accounts, count useful items actually observed in Group Storage")
    default boolean useGroupStorage() { return true; }

    @ConfigItem(keyName = "bankAware", name = "Bank-aware strategy",
            description = "Use the most recent verified bank snapshot when weighing options")
    default boolean bankAware() { return true; }

    @ConfigItem(keyName = "collectionistMode", name = "Collectionist mode",
            description = "Give a little more weight to useful or near-complete collection-log opportunities")
    default boolean collectionistMode() { return false; }

    @ConfigItem(keyName = "riskWarnings", name = "Risk warnings",
            description = "Warn before irreversible or high-risk account actions")
    default boolean riskWarnings() { return true; }

    @ConfigItem(keyName = "birdhouseReminders", name = "Birdhouse reminders",
            description = "Remind when a birdhouse run is ready and show a preparation checklist")
    default boolean birdhouseReminders() { return true; }

    @ConfigItem(keyName = "herbRunReminders", name = "Herb run reminders",
            description = "Remind when an herb run is ready and show a preparation checklist")
    default boolean herbRunReminders() { return true; }

    @ConfigItem(keyName = "clueReminders", name = "Clue reminders",
            description = "Surface clues at good times without constantly nagging")
    default boolean clueReminders() { return true; }
}
