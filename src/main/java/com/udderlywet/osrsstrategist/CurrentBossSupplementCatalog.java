package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/**
 * Current boss-index coverage that supplements the original progression list.
 * Entries stay conservative and still require loadout/mechanics proof.
 */
@Singleton
public class CurrentBossSupplementCatalog
{
    private final List<PvmEncounterDefinition> encounters = new ArrayList<>();

    public CurrentBossSupplementCatalog()
    {
        e("The Hueycoatl", 70, 70, 65, 70, 65, 43, 65, false, true,
                "Group-capable Varlamore boss. Verify role, supplies and current encounter mechanics.",
                "Children of the Sun");
        e("Gemstone Crab", 70, 70, 65, 70, 65, 43, 65, false, false,
                "Verify current encounter access, style choice and supplies before recommending.");
        e("Deranged Archaeologist", 1, 1, 45, 55, 50, 43, 50, false, false,
                "Mid-level ranged/magic encounter; verify Fossil Island access and safe positioning.");
        e("The Mimic", 1, 1, 65, 70, 70, 43, 65, false, true,
                "Only available from the appropriate clue reward event. Treat the clue state itself as the access gate.");
        e("Hespori", 60, 60, 55, 50, 50, 43, 55, false, false,
                "Farming-gated sporadic boss. Verify a grown Hespori and appropriate melee/ranged supplies.");
        e("Skotizo", 70, 70, 65, 65, 60, 43, 65, false, true,
                "Requires a dark totem and Catacombs access; conserve rare access items on restricted accounts.");
        e("Shellbane Gryphon", 60, 60, 60, 55, 50, 43, 60, false, true,
                "Task-gated Slayer boss. Require 51 Slayer, 60 combat, Troubled Tortugans progression and the correct Gryphon task.");
        e("Zalcano", 1, 1, 1, 1, 1, 1, 1, false, false,
                "Skilling boss. Prifddinas access plus Mining/Smithing/Runecraft readiness matter more than combat stats.",
                "Song of the Elves");
        e("Tempoross", 1, 1, 1, 1, 1, 1, 1, false, false,
                "Fishing skilling boss. Verify Fishing level, equipment and activity access.");
        e("Wintertodt", 1, 1, 1, 1, 1, 1, 10, false, false,
                "Firemaking skilling boss. Verify 50 Firemaking, warm clothing and food; Hardcore accounts use conservative food margins.");
    }

    public List<PvmEncounterDefinition> all()
    {
        return Collections.unmodifiableList(encounters);
    }

    private void e(String id, int attack, int strength, int defence,
            int ranged, int magic, int prayer, int hp,
            boolean wilderness, boolean highRisk, String note, String... quests)
    {
        encounters.add(new PvmEncounterDefinition(
                id, attack, strength, defence, ranged, magic, prayer, hp,
                Arrays.asList(quests), wilderness, highRisk, note));
    }
}
