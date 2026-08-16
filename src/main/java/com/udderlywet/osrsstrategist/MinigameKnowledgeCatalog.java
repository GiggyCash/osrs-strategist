package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/** Common progression minigames and the account value they provide. */
@Singleton
public class MinigameKnowledgeCatalog
{
    private final List<MinigameKnowledgeDefinition> definitions = new ArrayList<>();

    public MinigameKnowledgeCatalog()
    {
        add("guardians_of_the_rift", "Guardians of the Rift",
                "Runecraft XP, runes, Raiments of the Eye and Abyssal needle progression", 16, true);
        add("tempoross", "Tempoross",
                "Fishing XP, fish supplies and collection rewards", 13, true);
        add("wintertodt", "Wintertodt",
                "Firemaking XP, supplies and collection rewards", 13, true);
        add("giants_foundry", "Giants' Foundry",
                "Material-efficient Smithing XP and Smiths' Uniform progression", 15, true);
        add("mahogany_homes", "Mahogany Homes",
                "Material-efficient Construction XP and carpenter reward progression", 14, true);
        add("barbarian_assault", "Barbarian Assault",
                "Fighter torso, diary requirements and role progression", 15, true);
        add("pest_control", "Pest Control",
                "Void equipment and combat XP", 13, true);
        add("mage_training_arena", "Mage Training Arena",
                "Magic progression and unique rewards", 10, true);
        add("tithe_farm", "Tithe Farm",
                "Active Farming XP and Farming reward unlocks", 12, true);
        add("hallowed_sepulchre", "Hallowed Sepulchre",
                "High-intensity Agility XP, loot and collection progression", 15, true);
        add("motherlode_mine", "Motherlode Mine",
                "Low-attention Mining, ores and Prospector progression", 13, true);
        add("blast_furnace", "Blast Furnace",
                "Fast/resource-efficient bar processing depending ore and account mode", 12, false);
        add("volcanic_mine", "Volcanic Mine",
                "High-level Mining XP and volcanic rewards", 13, false);
        add("soul_wars", "Soul Wars",
                "Combat minigame rewards and collection opportunities", 7, false);
        add("castle_wars", "Castle Wars",
                "Cosmetic/collection progression", 5, false);
        add("trouble_brewing", "Trouble Brewing",
                "Diary and collection/cosmetic progression", 7, false);
        add("last_man_standing", "Last Man Standing",
                "PvP practice and reward points without risking the account's normal gear", 8, false);
        add("nightmare_zone", "Nightmare Zone",
                "Imbue points and low-attention combat training where appropriate", 10, false);
    }

    public List<MinigameKnowledgeDefinition> all()
    {
        return Collections.unmodifiableList(definitions);
    }

    private void add(String id, String name, String purpose,
            double score, boolean protectedProgression)
    {
        definitions.add(new MinigameKnowledgeDefinition(
                id, name, purpose, score, protectedProgression));
    }
}
