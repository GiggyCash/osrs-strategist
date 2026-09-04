package compass;

import net.runelite.client.config.*;

@ConfigGroup(OsrsStrategistConfig.GROUP)
public interface OsrsStrategistConfig extends Config
{
    String GROUP = "osrs-strategist";

    @ConfigSection(
            name = "Advanced",
            description = "Rare recovery actions",
            position = 100,
            closedByDefault = true)
    String advancedSection = "advanced";

    @ConfigItem(keyName = CompassConfigKeys.STRATEGY_MODE, name = "Strategy style",
            description = "How Compass weighs speed versus comfort")
    default StrategyMode strategyMode() { return StrategyMode.BALANCED; }

    @ConfigItem(keyName = CompassConfigKeys.SESSION_INTENT, name = "Session intent",
            description = "What kind of session you want Compass to plan for")
    default SessionIntent sessionIntent() { return SessionIntent.PICK_FOR_ME; }

    @ConfigItem(keyName = CompassConfigKeys.ACTIVE_GOAL, name = "Goal",
            description = "The long-term goal used to prioritize recommendations; Automatic applies no specific-goal bias")
    default PlayerGoal activeGoal() { return PlayerGoal.AUTOMATIC; }

    @ConfigItem(keyName = CompassConfigKeys.QUEST_TOLERANCE, name = "Optional quests",
            description = "How often elective quest detours should appear; quests required for the selected goal remain eligible")
    default QuestTolerance questTolerance() { return QuestTolerance.NORMAL; }

    @ConfigItem(keyName = CompassConfigKeys.ALLOW_WILDERNESS, name = "Wilderness methods",
            description = "Allow Compass to recommend methods that require entering the Wilderness")
    default boolean allowWildernessMethods() { return false; }

    @ConfigItem(keyName = CompassConfigKeys.DETAILS_OVERLAY, name = "Details overlay",
            description = "Allow the compact Why and current-step overlay")
    default boolean showDetailsOverlay() { return true; }

    @ConfigItem(keyName = CompassConfigKeys.METHOD_OVERLAY, name = "Method Guidance overlay",
            description = "Show the current method as a movable heads-up reference")
    default boolean showInGameGuidance() { return false; }

    @ConfigItem(keyName = CompassConfigKeys.SIDEBAR_TEXT_SIZE,
            name = "Sidebar text size",
            description = "Scale Compass text without changing the rest of RuneLite")
    default SidebarTextSize sidebarTextSize() { return SidebarTextSize.STANDARD; }

    @ConfigItem(keyName = CompassConfigKeys.USE_GROUP_STORAGE, name = "Use Group Storage",
            description = "For GIM accounts, count useful items actually observed in Group Storage")
    default boolean useGroupStorage() { return true; }

    @ConfigItem(keyName = CompassConfigKeys.COLLECTIONIST, name = "Collectionist mode",
            description = "Give a little more weight to useful or near-complete collection-log opportunities")
    default boolean collectionistMode() { return false; }

    @ConfigItem(
            keyName = CompassConfigKeys.RESET_LEARNED_FEEDBACK,
            name = "Reset learned feedback",
            description = "Clear Later, Not Today, and Dislike learning for the current character only; goals, settings, bank observations, and recommendation history are kept",
            section = advancedSection,
            warning = "Reset learned recommendation feedback for the current character? This clears Later, Not Today, and Dislike learning.")
    default boolean resetLearnedFeedback() { return false; }

    @ConfigItem(keyName = CompassConfigKeys.FIRST_USE_COMPLETE,
            name = "First use complete",
            description = "Internal first-use hint state", hidden = true)
    default boolean firstUseComplete() { return false; }
}
