package com.udderlywet.osrsstrategist;

import java.util.*;

/**
 * Hard membership boundary for quest recommendations.
 *
 * <p>RuneLite exposes the complete quest list even while the character is on a
 * free-to-play account/world. The Compass must therefore filter by content
 * entitlement before scoring quests. This list tracks the current F2P quest set
 * and intentionally fails closed for unknown names on F2P.</p>
 */
public final class QuestMembershipPolicy
{
    private static final Set<String> FREE_TO_PLAY_QUESTS =
            PolicyLists.normalizedSet(PolicyLists.DATA.free_to_play_quests);

    private QuestMembershipPolicy()
    {
    }

    public static boolean isAvailable(String questName, MembershipStatus membership)
    {
        if (questName == null || questName.trim().isEmpty()) return false;
        if (membership == MembershipStatus.P2P) return true;
        return FREE_TO_PLAY_QUESTS.contains(normalize(questName));
    }

    public static boolean isFreeToPlayQuest(String questName)
    {
        return questName != null && FREE_TO_PLAY_QUESTS.contains(normalize(questName));
    }

    public static Set<String> freeToPlayQuestNames()
    {
        return FREE_TO_PLAY_QUESTS;
    }

    private static String normalize(String value)
    {
        return PolicyLists.normalize(value);
    }
}
