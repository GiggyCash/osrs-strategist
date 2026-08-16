package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/**
 * Broad practical-readiness catalog for OSRS PvM.
 *
 * <p>Thresholds are deliberately conservative "reasonable first attempt"
 * envelopes, not claims that lower-level kills are impossible. Gear, supplies,
 * mechanics, invocation/scale and player experience remain separate readiness
 * dimensions.</p>
 */
@Singleton
public class PvmEncounterCatalog
{
    private final List<PvmEncounterDefinition> encounters = new ArrayList<>();

    public PvmEncounterCatalog()
    {
        // F2P / early bosses.
        e("Obor", 40, 40, 35, 1, 1, 37, 40, false, false,
                "Bring food and handle the key-gated encounter safely.");
        e("Bryophyta", 40, 40, 35, 1, 1, 37, 40, false, false,
                "Bring an axe and food; mossy key required.");
        e("Scurrius", 40, 40, 35, 40, 40, 43, 45, false, false,
                "Entry-level mechanics practice; choose a style the account can sustain.");
        e("Giant Mole", 60, 60, 55, 50, 50, 43, 55, false, false,
                "Protection prayer and a reliable tracking/return route improve comfort.");
        e("Barrows", 60, 60, 60, 55, 55, 43, 55, false, false,
                "Prayer management, a magic option and food are more important than one static BIS set.");
        e("Sarachnis", 65, 65, 60, 50, 50, 43, 60, false, false,
                "Crush-focused melee and prayer-switching make this a useful midgame mechanics step.");
        e("Amoxliatl", 65, 65, 60, 50, 50, 43, 60, false, false,
                "Use a verified quest/access route and bring a sustainable melee setup.");
        e("Royal Titans", 65, 65, 60, 65, 65, 43, 60, false, false,
                "Dual-style mechanics encounter; verify access and supplies before recommending.");

        // Classic bosses.
        e("Dagannoth Rex", 60, 60, 60, 1, 60, 43, 60, false, true,
                "Safespotting reduces mechanical load, but the trip to the arena remains dangerous.");
        e("Dagannoth Kings", 75, 75, 70, 75, 75, 70, 70, false, true,
                "Tribrid encounter; require sustainable gear, prayer and a safe entry plan.");
        e("Kalphite Queen", 80, 80, 75, 75, 70, 70, 75, false, true,
                "High incoming damage. Hardcore accounts should not receive this as a casual progression task.");
        e("King Black Dragon", 70, 70, 65, 65, 65, 43, 65, true, true,
                "Boss room is not Wilderness, but reaching it requires Wilderness exposure.");
        e("Chaos Elemental", 75, 75, 70, 70, 70, 43, 70, true, true,
                "Wilderness boss with player-killer exposure.");
        e("Crazy Archaeologist", 1, 1, 40, 50, 50, 43, 45, true, true,
                "Wilderness exposure is the primary account-risk consideration.");
        e("Chaos Fanatic", 1, 1, 40, 50, 50, 43, 45, true, true,
                "Wilderness exposure is the primary account-risk consideration.");
        e("Calvar'ion", 70, 70, 60, 1, 1, 43, 60, true, true,
                "Wilderness escape plan and low-risk carried value are required.");
        e("Spindel", 70, 70, 60, 65, 1, 43, 60, true, true,
                "Wilderness escape plan and low-risk carried value are required.");
        e("Artio", 1, 1, 60, 70, 65, 43, 60, true, true,
                "Wilderness escape plan and low-risk carried value are required.");
        e("Callisto", 1, 1, 70, 80, 75, 70, 70, true, true,
                "Multi-combat Wilderness boss; strong risk controls required.");
        e("Venenatis", 80, 80, 70, 75, 1, 70, 70, true, true,
                "Multi-combat Wilderness boss; strong risk controls required.");
        e("Vet'ion", 80, 80, 70, 1, 1, 70, 70, true, true,
                "Multi-combat Wilderness boss; strong risk controls required.");

        // God Wars and major group bosses.
        e("General Graardor", 80, 80, 75, 70, 70, 70, 75, false, true,
                "God Wars access, killcount/keys, sustain and role-specific gear required.");
        e("K'ril Tsutsaroth", 80, 80, 75, 70, 70, 70, 75, false, true,
                "High damage potential; require God Wars access and a sustainable trip plan.");
        e("Commander Zilyana", 70, 70, 75, 80, 70, 70, 75, false, true,
                "Ranged/kiting readiness and God Wars access required.");
        e("Kree'arra", 1, 1, 75, 80, 1, 70, 75, false, true,
                "Ranged-heavy encounter with expensive supply and gear considerations.");
        e("Nex", 80, 80, 80, 85, 80, 74, 80, false, true,
                "High-level group PvM. Require Frozen Door access, role-appropriate gear and team expectations.");
        e("Corporeal Beast", 80, 80, 75, 70, 70, 70, 75, false, true,
                "High-stat boss with specialized weapon/team considerations.");
        e("The Nightmare", 80, 80, 75, 80, 80, 70, 75, false, true,
                "Long mechanics-heavy fight with significant supply demands.");
        e("Phosani's Nightmare", 90, 90, 85, 85, 85, 77, 85, false, true,
                "Solo high-punishment encounter; mechanics proficiency required.");

        // Quest/unlock bosses.
        e("Zulrah", 1, 1, 70, 75, 75, 43, 70, false, true,
                "Rotation learning and ranged/magic gear swaps required.", "Regicide");
        e("Vorkath", 75, 75, 70, 80, 70, 70, 75, false, true,
                "Dragonfire protection, crumble-undead response and movement mechanics required.", "Dragon Slayer II");
        e("Phantom Muspah", 1, 1, 70, 80, 80, 70, 75, false, true,
                "Ranged/magic or strong ranged-only setup plus prayer management required.", "Secrets of the North");
        e("The Leviathan", 1, 1, 75, 85, 75, 74, 80, false, true,
                "Fast prayer-switching and movement mechanics; post-DT2 access required.", "Desert Treasure II - The Fallen Empire");
        e("The Whisperer", 1, 1, 75, 1, 85, 74, 80, false, true,
                "Magic-focused mechanics and sanity management; post-DT2 access required.", "Desert Treasure II - The Fallen Empire");
        e("Vardorvis", 85, 85, 75, 1, 1, 70, 80, false, true,
                "Fast movement/prayer mechanics; post-DT2 access required.", "Desert Treasure II - The Fallen Empire");
        e("Duke Sucellus", 80, 80, 75, 1, 1, 70, 80, false, true,
                "Prep phase and melee mechanics; post-DT2 access required.", "Desert Treasure II - The Fallen Empire");
        e("Yama", 80, 80, 75, 80, 80, 77, 80, false, true,
                "High-level multi-style mechanics. Base access requires A Kingdom Divided; contracts can be substantially harder.", "A Kingdom Divided");
        e("Doom of Mokhaiotl", 85, 85, 80, 85, 85, 77, 80, false, true,
                "Late-game mechanically demanding encounter. Require verified current access and gear before recommending.");
        e("Araxxor", 80, 80, 75, 75, 70, 70, 75, false, true,
                "High-level Slayer boss; require the appropriate Slayer assignment and task unlock/state.");
        e("Alchemical Hydra", 80, 80, 75, 85, 75, 74, 80, false, true,
                "High Slayer requirement and movement/prayer mechanics; only on the proper task.");
        e("Cerberus", 80, 80, 75, 75, 70, 70, 75, false, true,
                "High Slayer requirement, prayer drain and ghost mechanics; only on the proper task.");
        e("Abyssal Sire", 80, 80, 70, 75, 75, 70, 75, false, true,
                "High Slayer requirement and multi-phase mechanics; only on the proper task.");
        e("Grotesque Guardians", 75, 75, 70, 75, 1, 70, 70, false, true,
                "Gargoyle Slayer task and multi-style mechanics required.");
        e("Kraken", 1, 1, 60, 1, 75, 43, 60, false, false,
                "Task-gated low-mechanics Slayer boss; verify Slayer assignment.");
        e("Thermonuclear Smoke Devil", 80, 80, 70, 1, 1, 70, 70, false, true,
                "Task-gated Slayer boss; verify smoke-devil assignment and protection.");

        // Minigame/combat challenges.
        e("TzTok-Jad", 1, 1, 70, 70, 60, 43, 70, false, true,
                "Fight Cave endurance and prayer switching. Recommend practice/supplies before a first cape.");
        e("TzKal-Zuk", 1, 1, 90, 90, 90, 77, 90, false, true,
                "Inferno is an endgame endurance challenge; gear and mechanics mastery dominate raw stats.");
        e("Sol Heredit", 85, 85, 85, 80, 80, 77, 85, false, true,
                "Colosseum end encounter; require demonstrated wave progression and high-end preparation.");
        e("Crystalline Hunllef", 70, 70, 70, 70, 70, 43, 70, false, true,
                "Gauntlet is self-contained, so external gear is irrelevant; mechanics and Prifddinas access matter.", "Song of the Elves");
        e("Corrupted Hunllef", 80, 80, 80, 80, 80, 70, 80, false, true,
                "Corrupted Gauntlet is self-contained but mechanically demanding; require Prifddinas access.", "Song of the Elves");
        e("Moons of Peril", 70, 70, 70, 60, 60, 43, 65, false, false,
                "Midgame self-contained supply loop with melee-focused encounter mechanics.");

        // Raids. Current OSRS has three raid families; modes/scales are separate later.
        e("Chambers of Xeric", 80, 80, 75, 80, 80, 70, 75, false, true,
                "Three-style raid readiness, role knowledge and team/solo scale determine practical difficulty.");
        e("Theatre of Blood Entry Mode", 75, 75, 70, 75, 75, 70, 70, false, true,
                "Learning-oriented Theatre route; mechanics still matter more than raw combat level.");
        e("Theatre of Blood", 90, 90, 85, 90, 85, 77, 85, false, true,
                "Endgame team mechanics, role gear and encounter proficiency required.");
        e("Tombs of Amascut Entry Mode", 70, 70, 65, 70, 70, 43, 65, false, true,
                "Low invocation learning path; require Beneath Cursed Sands access.", "Beneath Cursed Sands");
        e("Tombs of Amascut", 80, 80, 75, 80, 80, 70, 75, false, true,
                "Invocation level changes readiness substantially; require Beneath Cursed Sands access.", "Beneath Cursed Sands");
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
