package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Singleton;

/**
 * High-value quest progression metadata layered on RuneLite's complete live
 * quest-state reader. Quests not present here are still visible to the generic
 * provider; they simply remain CHECK_NEEDED for deeper prerequisite reasoning.
 */
@Singleton
public class QuestKnowledgeCatalog
{
    private final Map<String, QuestKnowledgeDefinition> definitions = new HashMap<>();

    public QuestKnowledgeCatalog()
    {
        // F2P / universal foundations.
        q("Cook's Assistant", 7, "Early quest points and basic account progression", false, false);
        q("The Restless Ghost", 10, "Prayer experience and early quest progression", false, false);
        q("Rune Mysteries", 12, "Runecraft progression and rune-related access", false, false);
        q("The Knight's Sword", 13, "Large early Smithing experience reward", false, false);
        q("Dragon Slayer I", 17, "Major early combat milestone and equipment progression", false, true,
                "Demon Slayer");
        q("Below Ice Mountain", 9, "Camdozaal skilling and collection content", false, false);

        // Early members progression.
        q("Druidic Ritual", 20, "Unlocks Herblore", true, false);
        q("Waterfall Quest", 20, "Major early Attack and Strength progression", true, true);
        q("Tree Gnome Village", 17, "Combat experience and spirit-tree progression", true, true);
        q("The Grand Tree", 19, "Gnome transport, combat experience and progression", true, true,
                "Tree Gnome Village");
        q("Fight Arena", 15, "Fast early combat experience", true, true);
        q("Priest in Peril", 19, "Unlocks Morytania", true, true,
                "The Restless Ghost");
        q("Lost City", 17, "Zanaris and dragon weapon progression", true, true);
        q("Fairytale I - Growing Pains", 18, "Farming progression and fairy-ring prerequisite", true, true,
                "Lost City");
        q("Fairytale II - Cure a Queen", 22, "Fairy-ring transport access during progression", true, true,
                "Fairytale I - Growing Pains");
        q("The Tourist Trap", 16, "Dart Smithing/Fletching unlock and Agility experience options", true, true);
        q("Animal Magnetism", 20, "Ava's devices and ranged equipment progression", true, true,
                "The Restless Ghost", "Ernest the Chicken", "Priest in Peril");
        q("Death Plateau", 13, "Troll-area progression and climbing boots access", true, true);
        q("Troll Stronghold", 16, "Troll-area progression and later quest prerequisite", true, true,
                "Death Plateau");
        q("The Dig Site", 16, "Fossil/Varrock progression and several later quest chains", true, false);
        q("Bone Voyage", 19, "Unlocks Fossil Island", true, false,
                "The Dig Site");
        q("Ghosts Ahoy", 17, "Ectophial transport and Morytania utility", true, true,
                "Priest in Peril");

        // Midgame quest backbone.
        q("Monkey Madness I", 22, "Major melee weapon and Ape Atoll progression", true, true,
                "The Grand Tree", "Tree Gnome Village");
        q("Heroes' Quest", 18, "Heroes' Guild and later quest progression", true, true);
        q("Legends' Quest", 20, "Legends' Guild and major quest-chain progression", true, true,
                "Heroes' Quest");
        q("Underground Pass", 19, "Elf quest-line progression and Iban's staff", true, true);
        q("Regicide", 20, "Tirannwn progression", true, true,
                "Underground Pass");
        q("Roving Elves", 18, "Crystal equipment and elf progression", true, true,
                "Regicide");
        q("Mourning's End Part I", 20, "Elf quest-line progression", true, true,
                "Roving Elves");
        q("Mourning's End Part II", 22, "Death altar and elf quest-line progression", true, true,
                "Mourning's End Part I");
        q("Recipe for Disaster", 27, "Barrows gloves and broad account progression", true, true,
                "Cook's Assistant");
        q("Desert Treasure I", 25, "Ancient Magicks", true, true);
        q("Lunar Diplomacy", 23, "Lunar spellbook", true, true);
        q("Dream Mentor", 18, "Additional Lunar spells and combat progression", true, true,
                "Lunar Diplomacy");
        q("King's Ransom", 22, "Knight Waves and Piety-family prayer progression", true, true);
        q("Swan Song", 18, "Piscatoris and monkfish access", true, true);
        q("Cabin Fever", 18, "Mos Le'Harmless and cave-horror/pirate progression", true, true);
        q("A Taste of Hope", 19, "Vampyre progression and Theatre-region access", true, true);
        q("A Kingdom Divided", 22, "Kourend progression and spellbook utility", true, true);
        q("Beneath Cursed Sands", 24, "Tombs of Amascut access", true, true);
        q("Secrets of the North", 24, "Phantom Muspah and Mahjarrat progression", true, true);

        // Major late-game unlocks.
        q("Song of the Elves", 30, "Prifddinas, Gauntlet, crystal progression and major skilling access", true, true,
                "Mourning's End Part II");
        q("Dragon Slayer II", 30, "Vorkath, Myth's Guild and major PvM progression", true, true,
                "Dragon Slayer I", "Legends' Quest");
        q("Monkey Madness II", 27, "Demonic gorillas and advanced Ape Atoll progression", true, true,
                "Monkey Madness I");
        q("Sins of the Father", 27, "Darkmeyer, Hallowed Sepulchre and vampyre progression", true, true,
                "A Taste of Hope");
        q("Desert Treasure II - The Fallen Empire", 32, "Ancient bosses, vestiges and endgame Mahjarrat progression", true, true,
                "Desert Treasure I", "Secrets of the North");
        q("While Guthix Sleeps", 30, "Tormented demons and major grandmaster progression", true, true);
    }

    public QuestKnowledgeDefinition get(String questName)
    {
        if (questName == null) return null;
        return definitions.get(normalize(questName));
    }

    public List<QuestKnowledgeDefinition> all()
    {
        return Collections.unmodifiableList(new ArrayList<>(definitions.values()));
    }

    private void q(String name, double score, String unlock,
            boolean members, boolean risky, String... prerequisites)
    {
        definitions.put(normalize(name), new QuestKnowledgeDefinition(
                name, score, unlock, Arrays.asList(prerequisites), members, risky));
    }

    private static String normalize(String value)
    {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
