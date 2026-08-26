package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Central config-event routing prevents cosmetic toggles from reranking. */
final class CompassConfigKeys
{
    static final String STRATEGY_MODE = "strategyMode";
    static final String SESSION_INTENT = "sessionIntent";
    static final String ACTIVE_GOAL = "activeGoal";
    static final String QUEST_TOLERANCE = "questTolerance";
    static final String ALLOW_WILDERNESS = "allowWildernessMethods";
    static final String USE_GROUP_STORAGE = "useGroupStorage";
    static final String COLLECTIONIST = "collectionistMode";
    static final String DETAILS_OVERLAY = "showDetailsOverlay";
    static final String METHOD_OVERLAY = "showInGameGuidance";
    static final String SIDEBAR_TEXT_SIZE = "sidebarTextSize";
    static final String FIRST_USE_COMPLETE = "firstUseComplete";

    private static final Set<String> PLANNING = new HashSet<>(Arrays.asList(
            STRATEGY_MODE, SESSION_INTENT, ACTIVE_GOAL, QUEST_TOLERANCE,
            ALLOW_WILDERNESS, USE_GROUP_STORAGE, COLLECTIONIST));
    private static final Set<String> PROFILE = new HashSet<>(Arrays.asList(
            STRATEGY_MODE, SESSION_INTENT, ACTIVE_GOAL, QUEST_TOLERANCE,
            ALLOW_WILDERNESS, USE_GROUP_STORAGE, COLLECTIONIST));

    private CompassConfigKeys() { }

    static boolean changesPlanning(String key) { return PLANNING.contains(key); }
    static boolean changesStrategyProfile(String key) { return PROFILE.contains(key); }
    static boolean isOverlay(String key)
    {
        return DETAILS_OVERLAY.equals(key) || METHOD_OVERLAY.equals(key);
    }
    static boolean acknowledgesFirstUse(String key)
    {
        return STRATEGY_MODE.equals(key) || SESSION_INTENT.equals(key)
                || ACTIVE_GOAL.equals(key);
    }
}
