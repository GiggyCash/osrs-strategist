package com.udderlywet.osrsstrategist;

import java.util.*;

/**
 * Quest safety gate for player-imposed account builds.
 *
 * <p>For a normal account, every quest remains eligible. For a protected build,
 * unknown reward profiles are treated as unsafe until curated. This is
 * intentionally fail-closed: missing a recommendation is recoverable; granting
 * irreversible Defence/Hitpoints/offensive experience to a pure is not.</p>
 */
public final class RestrictedQuestPolicy
{
    private static final Set<String> ONE_DEFENCE_SAFE = set(
            "Animal Magnetism",
            "Another Slice of H.A.M.",
            "Cook's Assistant",
            "The Corsair Curse",
            "Creature of Fenkenstrain",
            "Current Affairs",
            "Death on the Isle",
            "Death Plateau",
            "Desert Treasure I",
            "Dwarf Cannon",
            "Ghosts Ahoy",
            "The Giant Dwarf",
            "Goblin Diplomacy",
            "The Golem",
            "The Grand Tree",
            "The Great Brain Robbery",
            "Horror from the Deep",
            "Land of the Goblins",
            "Learning the Ropes",
            "Lost City",
            "Making History",
            "Misthalin Mystery",
            "Monk's Friend",
            "Monkey Madness I",
            "Monkey Madness II",
            "Mountain Daughter",
            "One Small Favour",
            "Pandemonium",
            "The Path of Glouphrie",
            "Perilous Moons",
            "Priest in Peril",
            "Rag and Bone Man I",
            "Rag and Bone Man II",
            "The Restless Ghost",
            Text.get(700),
            "Roving Elves",
            "Rum Deal",
            "Rune Mysteries",
            "Scorpion Catcher",
            "Shadows of Custodia",
            "Sheep Herder",
            "Sheep Shearer",
            "Shield of Arrav",
            "Spirits of the Elid",
            "Swan Song",
            "The Final Dawn",
            "The Ides of Milk",
            "The Red Reef",
            "The Tourist Trap",
            "Tower of Life",
            "Tree Gnome Village",
            "Tribal Totem",
            "Troll Romance",
            "Waterfall Quest",
            "Witch's Potion"
    );

    /** No forced combat-skill or Prayer reward in these baseline quests. */
    private static final Set<String> LEVEL_THREE_SAFE = set(
            "Cook's Assistant",
            "The Corsair Curse",
            "Current Affairs",
            "Death on the Isle",
            "The Giant Dwarf",
            "Goblin Diplomacy",
            "The Golem",
            "Land of the Goblins",
            "Learning the Ropes",
            "Misthalin Mystery",
            "Monk's Friend",
            "One Small Favour",
            "Pandemonium",
            Text.get(701),
            "Rune Mysteries",
            "Shadows of Custodia",
            "Sheep Herder",
            "Sheep Shearer",
            "Shield of Arrav",
            "The Tourist Trap",
            "Tower of Life",
            "Tribal Totem"
    );

    /** Adds quests whose forced combat reward is Prayer only. */
    private static final Set<String> PRAYER_SKILLER_EXTRA = set(
            "The Restless Ghost",
            "Making History",
            "Ghosts Ahoy",
            "Mountain Daughter",
            "Priest in Peril",
            "Rag and Bone Man I",
            "Rag and Bone Man II",
            "Rum Deal",
            "Spirits of the Elid"
    );

    private RestrictedQuestPolicy() {}

    public static boolean isSafe(AccountSnapshot account, String questName)
    {
        if (account == null || questName == null) return false;
        RestrictedBuildType build = AccountBuildPolicy.effectiveBuild(account);
        if (build == RestrictedBuildType.STANDARD
                || build == RestrictedBuildType.RANGE_TANK
                || build == RestrictedBuildType.MED_BUILD
                || build == RestrictedBuildType.COMBAT_ONLY)
        {
            return true;
        }

        String quest = normalize(questName);
        switch (build)
        {
            case SKILLER:
            case F2P_SKILLER:
                return contains(LEVEL_THREE_SAFE, quest);

            case PRAYER_SKILLER:
                return contains(LEVEL_THREE_SAFE, quest)
                        || contains(PRAYER_SKILLER_EXTRA, quest);

            case ONE_DEFENCE_PURE:
            case LOW_DEFENCE_PURE:
            case INITIATE_PURE:
            case RUNE_PURE:
            case VOID_PURE:
            case ZERKER:
            case OBSIDIAN_MAULER:
                return contains(ONE_DEFENCE_SAFE, quest);

            case DEFENCE_PURE:
                // A Defence pure must not accidentally gain Attack, Strength,
                // Ranged, Magic, Hitpoints, or Slayer from quest rewards.
                return contains(LEVEL_THREE_SAFE, quest)
                        || contains(PRAYER_SKILLER_EXTRA, quest);

            case TEN_HITPOINTS:
                // This conservative baseline omits quests with known forced HP
                // rewards. More 10-HP-safe quest routes can be curated without
                // weakening the irreversible safety boundary.
                return contains(LEVEL_THREE_SAFE, quest)
                        || contains(PRAYER_SKILLER_EXTRA, quest);

            default:
                return false;
        }
    }

    private static Set<String> set(String... values)
    {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    private static boolean contains(Set<String> values, String normalized)
    {
        for (String value : values)
        {
            if (normalize(value).equals(normalized)) return true;
        }
        return false;
    }

    private static String normalize(String value)
    {
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("\\s+", " ");
    }
}
