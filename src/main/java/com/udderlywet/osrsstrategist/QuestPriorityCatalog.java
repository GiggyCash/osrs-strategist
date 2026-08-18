package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Compass weighting for quests with unusually important unlocks.
 * The complete quest identity/state list still comes from RuneLite Quest.values().
 */
@Singleton
public class QuestPriorityCatalog
{
    private final Map<String, QuestPriority> priorities = new HashMap<>();

    public QuestPriorityCatalog()
    {
        unlock("Druidic Ritual", 14, "Unlocks Herblore progression");
        unlock("Rune Mysteries", 7, "Runecraft progression unlock");
        unlock("Pandemonium", 18, "Unlocks Sailing progression");
        unlock("Prying Times", 8, "Expands Sailing sea charting");
        unlock("Current Affairs", 8, "Expands Sailing sea charting");
        unlock("Tears of Guthix", 9, "Unlocks a recurring lowest-skill XP opportunity");
        unlock("Animal Magnetism", 12, "Unlocks Ava's ranged devices");
        unlock("Dwarf Cannon", 10, "Unlocks the dwarf multicannon");
        unlock("Priest in Peril", 12, "Unlocks Morytania");
        unlock("Bone Voyage", 14, "Unlocks Fossil Island");
        unlock("Children of the Sun", 12, "Unlocks Varlamore");
        unlock("Throne of Miscellania", 10, "Unlocks Managing Miscellania");
        unlock("Royal Trouble", 7, "Improves Managing Miscellania");
        unlock("Fairytale I - Growing Pains", 9, "Core Farming and fairy-ring progression");
        unlock("Fairytale II - Cure a Queen", 15, "Unlocks fairy-ring transport early in the quest");
        unlock("Lost City", 10, "Unlocks Zanaris and dragon weapon progression");
        unlock("Monkey Madness I", 12, "Major melee weapon and area progression");
        unlock("Monkey Madness II", 14, "Major combat, transport, and PvM progression");
        unlock("Desert Treasure I", 16, "Unlocks Ancient Magicks");
        unlock("Desert Treasure II - The Fallen Empire", 16, "Unlocks major late-game PvM progression");
        unlock("Lunar Diplomacy", 14, "Unlocks the Lunar spellbook");
        unlock("Dream Mentor", 8, "Unlocks additional Lunar spells");
        unlock("A Kingdom Divided", 12, "Unlocks major Arceuus spellbook progression");
        unlock("King's Ransom", 13, "Unlocks the route to Chivalry and Piety");
        unlock("Recipe for Disaster", 18, "Progresses Culinaromancer's gloves toward Barrows gloves");
        unlock("Dragon Slayer I", 10, "Major early combat equipment progression");
        unlock("Dragon Slayer II", 18, "Unlocks Vorkath, Ava's assembler, ferocious gloves, and Mythical cape progression");
        unlock("Regicide", 10, "Unlocks deeper Tirannwn progression");
        unlock("Mourning's End Part I", 8, "Unlocks Lletya progression");
        unlock("Song of the Elves", 22, "Unlocks Prifddinas and major account progression");
        unlock("Sins of the Father", 16, "Unlocks Darkmeyer and major Morytania progression");
        unlock("Beneath Cursed Sands", 17, "Unlocks Tombs of Amascut");
        unlock("Perilous Moons", 11, "Unlocks Cam Torum and Neypotzli progression");
        unlock("Below Ice Mountain", 8, "Unlocks the Ruins of Camdozaal");
        unlock("The Fremennik Trials", 10, "Unlocks core Fremennik progression");
        unlock("The Fremennik Isles", 9, "Unlocks helm and Fremennik equipment progression");
        unlock("Cabin Fever", 8, "Unlocks Mos Le'Harmless and pirate progression");
        unlock("Making Friends with My Arm", 9, "Unlocks Weiss and basalt transport progression");
        unlock("Tai Bwo Wannai Trio", 8, "Unlocks cooked karambwan progression");
        unlock("The Slug Menace", 7, "Unlocks proselyte armour");
        unlock("Family Crest", 7, "Unlocks Family gauntlet progression");
        unlock("Watchtower", 7, "Unlocks Watchtower Teleport and deeper ogre-area progression");
    }

    public QuestPriority priorityFor(String questName)
    {
        return priorities.get(normalize(questName));
    }

    public Map<String, QuestPriority> snapshot()
    {
        return Collections.unmodifiableMap(priorities);
    }

    private void unlock(String name, double bonus, String reason)
    {
        priorities.put(normalize(name), new QuestPriority(name, bonus, reason));
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    public static final class QuestPriority
    {
        private final String name;
        private final double scoreBonus;
        private final String reason;

        QuestPriority(String name, double scoreBonus, String reason)
        {
            this.name = name;
            this.scoreBonus = scoreBonus;
            this.reason = reason;
        }

        public String getName() { return name; }
        public double getScoreBonus() { return scoreBonus; }
        public String getReason() { return reason; }
    }
}
