package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

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
    private static final Set<String> FREE_TO_PLAY_QUESTS;

    static
    {
        Set<String> quests = new LinkedHashSet<>();
        add(quests,
                "Learning the Ropes",
                "The Ides of Milk",
                "Below Ice Mountain",
                "Black Knights' Fortress",
                "Cook's Assistant",
                "The Corsair Curse",
                "Demon Slayer",
                "Doric's Quest",
                "Dragon Slayer I",
                "Ernest the Chicken",
                "Goblin Diplomacy",
                "Imp Catcher",
                "Misthalin Mystery",
                "Pirate's Treasure",
                "Prince Ali Rescue",
                "Romeo & Juliet",
                "Rune Mysteries",
                "Sheep Shearer",
                "Shield of Arrav",
                "The Knight's Sword",
                "The Restless Ghost",
                "Vampyre Slayer",
                "Witch's Potion",
                "X Marks the Spot");
        FREE_TO_PLAY_QUESTS = Collections.unmodifiableSet(quests);
    }

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

    private static void add(Set<String> target, String... names)
    {
        if (names == null) return;
        for (String name : names)
        {
            if (name != null) target.add(normalize(name));
        }
    }

    private static String normalize(String value)
    {
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("\\s+", " ");
    }
}
