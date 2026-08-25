package com.udderlywet.osrsstrategist;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OsrsStrategistConfig.GROUP)
public interface OsrsStrategistConfig extends Config
{
    String GROUP = "osrs-strategist";

    @ConfigItem(keyName = CompassConfigKeys.STRATEGY_MODE, name = "Strategy style",
            description = "How Compass weighs speed versus comfort")
    default StrategyMode strategyMode() { return StrategyMode.BALANCED; }

    @ConfigItem(keyName = CompassConfigKeys.SESSION_INTENT, name = "Session intent",
            description = "What kind of session you want Compass to plan for")
    default SessionIntent sessionIntent() { return SessionIntent.PICK_FOR_ME; }

    @ConfigItem(keyName = CompassConfigKeys.ACTIVE_GOAL, name = "Goal",
            description = "The long-term goal used to prioritize recommendations; Max is the automatic all-account path")
    default GoalType activeGoal() { return GoalType.MAX; }

    @ConfigItem(keyName = CompassConfigKeys.QUEST_TOLERANCE, name = "Quest tolerance",
            description = "How often quests should appear in recommendations")
    default QuestTolerance questTolerance() { return QuestTolerance.NORMAL; }

    @ConfigItem(keyName = CompassConfigKeys.ALLOW_WILDERNESS, name = "Wilderness methods",
            description = "Allow Compass to recommend methods that require entering the Wilderness")
    default boolean allowWildernessMethods() { return false; }

    @ConfigItem(keyName = CompassConfigKeys.DETAILS_OVERLAY, name = "Details overlay",
            description = "Allow the compact Why and current-step overlay")
    default boolean showDetailsOverlay() { return true; }

    @ConfigItem(keyName = CompassConfigKeys.METHOD_OVERLAY, name = "Method Guidance overlay",
            description = "Show the current method as a movable heads-up reference")
    default boolean showInGameGuidance() { return true; }

    @ConfigItem(keyName = CompassConfigKeys.USE_GROUP_STORAGE, name = "Use Group Storage",
            description = "For GIM accounts, count useful items actually observed in Group Storage")
    default boolean useGroupStorage() { return true; }

    @ConfigItem(keyName = "bankAware", name = "Bank-aware strategy",
            description = "Use the most recent verified bank snapshot when weighing options")
    default boolean bankAware() { return true; }

    @ConfigItem(keyName = CompassConfigKeys.COLLECTIONIST, name = "Collectionist mode",
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

    @ConfigItem(keyName = CompassConfigKeys.FIRST_USE_COMPLETE,
            name = "First use complete",
            description = "Internal first-use hint state", hidden = true)
    default boolean firstUseComplete() { return false; }
}
