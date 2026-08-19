package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Encounter-specific preparation profiles backed by RuneLite identities and
 * locally curated access/risk evidence. These profiles can only produce
 * CHECK_NEEDED preparation; they cannot promote an account to VERIFIED.
 */
@Singleton
public class PvmPreparationProfileCatalog
{
    public static final String PROVENANCE =
            "RuneLite HiscoreSkill/Slayer Task 1.12.35 and OSRS Wiki encounter pages; audited 2026-08-19";
    private final Map<String, PvmPreparationProfile> profiles = new LinkedHashMap<>();

    public PvmPreparationProfileCatalog()
    {
        slayerBosses();
        wildernessBosses();
        conventionalBosses();
        skillingEncounters();
        currentEncounters();
    }

    public PvmPreparationProfile forActivity(String id)
    {
        return id == null ? null : profiles.get(id.toLowerCase());
    }

    public Map<String, PvmPreparationProfile> all()
    {
        return Collections.unmodifiableMap(profiles);
    }

    private void slayerBosses()
    {
        add("pvm:abyssal_sire", "melee or ranged",
                "Verify an abyssal-demon Slayer assignment and 85 Slayer",
                "Verify access to the Abyssal Nexus and a legal respiratory-system stun option",
                "Verify poison protection, restoration, food and an escape teleport");
        add("pvm:grotesque_guardians", "hybrid",
                "Verify a gargoyle Slayer assignment, 75 Slayer and access to the Slayer Tower roof",
                "Carry a brittle key for the first unlock when still required",
                "Prepare legal ranged and melee phases plus a rock hammer where applicable");
        add("pvm:thermonuclear_smoke_devil", "melee",
                "Verify a smoke-devil Slayer assignment and 93 Slayer",
                "Equip a facemask, Slayer helmet, or another verified smoke protection item",
                "Prepare restoration, food and an escape teleport");
    }

    private void wildernessBosses()
    {
        wild("artio", "ranged or magic");
        wild("callisto", "ranged or magic");
        wild("calvarion", "melee");
        wild("chaos_elemental", "ranged or melee");
        wild("chaos_fanatic", "ranged");
        wild("crazy_archaeologist", "ranged or magic");
        wild("scorpia", "magic");
        wild("spindel", "melee");
        wild("venenatis", "melee");
        wild("vetion", "melee");
    }

    private void conventionalBosses()
    {
        add("pvm:corporeal_beast", "melee",
                "Verify the Corporeal Beast cave route and a safe instance/team plan",
                "Use a verified spear-class damage option; do not infer suitability from a generic melee weapon",
                "Prepare high-tier food, restoration and a teleport; compare the setup cost with the account goal");
        add("pvm:dagannoth_prime", "ranged",
                "Verify Waterbirth Dungeon access and the route through the dungeon",
                "Prepare protection against the other Dagannoth Kings during entry",
                "Carry compatible ammunition, food, restoration and an emergency teleport");
        add("pvm:dagannoth_rex", "magic",
                "Verify Waterbirth Dungeon access and the route through the dungeon",
                "Prepare a safe lure and protection from the other Dagannoth Kings during entry",
                "Carry runes, food, restoration and an emergency teleport");
        add("pvm:dagannoth_supreme", "melee",
                "Verify Waterbirth Dungeon access and the route through the dungeon",
                "Prepare protection against the other Dagannoth Kings during entry",
                "Carry food, restoration and an emergency teleport");
        add("pvm:kalphite_queen", "hybrid",
                "Verify access to the Kalphite Lair and a usable rope route",
                "Prepare two legal combat styles for the encounter phases",
                "Carry food, restoration, poison protection and an emergency teleport");
        add("pvm:king_black_dragon", "ranged or melee",
                "Verify a route through the Wilderness to the KBD lair and explicit Wilderness risk acceptance",
                "Verify dragonfire protection and a compatible one-handed setup where required",
                "Carry food, restoration and a tested escape route");
        add("pvm:mimic", "combat",
                "Verify an active Mimic casket encounter rather than treating collection-log absence as access",
                "Prepare a legal high-damage setup for the selected combat style",
                "Carry food, restoration and an emergency teleport");
        add("pvm:nightmare", "melee",
                "Verify access to Sisterhood Sanctuary and the intended team or mass context",
                "Prepare crush-focused melee plus any encounter switch the selected role requires",
                "Carry food, restoration and a safe return/escape plan");
        add("pvm:phosanis_nightmare", "melee",
                "Verify completion/access for the solo Phosani encounter",
                "Prepare crush-focused melee and the required ranged or magic switch",
                "Treat player mechanical skill as unknown; verify food, restoration and escape before attempting");
        add("pvm:phantom_muspah", "ranged or magic",
                "Verify Secrets of the North completion and access to the encounter",
                "Prepare both phase-appropriate damage and prayer management",
                "Carry compatible ammunition/runes, restoration, food and an emergency teleport");
        add("pvm:skotizo", "melee",
                "Carry a dark totem and verify Catacombs of Kourend access",
                "Prepare a legal melee setup and a plan for the altar mechanics",
                "Carry food, restoration and an emergency teleport");
    }

    private void skillingEncounters()
    {
        add("pvm:tempoross", "skilling",
                "Verify 35 Fishing and Tempoross Cove access",
                "Carry the activity tools and verify the selected cooking or non-cooking route",
                "Use this for Fishing/reward progression, not as combat readiness");
        add("pvm:wintertodt", "skilling",
                "Verify 50 Firemaking and Great Kourend access",
                "Wear verified warm clothing and carry food appropriate to observed Hitpoints",
                "Use this for Firemaking/reward progression, not as combat readiness");
        add("pvm:zalcano", "skilling",
                "Verify Song of the Elves completion and Prifddinas access",
                "Prepare the required Mining, Smithing and Runecraft activity tools",
                "Carry food and an escape option; value the encounter for resources and collection goals");
    }

    private void currentEncounters()
    {
        add("pvm:amoxliatl", "crush melee",
                "Verify completion of The Heart of Darkness and access to the Twilight Temple",
                "Use a legal crush setup and prepare to destroy unstable ice with melee",
                "Carry food, restoration and an escape; do not infer mechanic readiness from stats alone");
        add("pvm:deranged_archaeologist", "magic",
                "Verify Bone Voyage completion and Fossil Island Tar Swamp access",
                "Carry an axe for the swamp route; add a rake or use an already unlocked alternate crossing",
                "Use Protect from Missiles at range and be ready to move immediately from the book special attack");
        add("pvm:doom_of_mokhaiotl", "ranged with switches",
                "Verify The Final Dawn completion and access to the Ruins of Mokhaiotl",
                "Start only at a conservative delve level; player execution and deeper-delve readiness remain unknown",
                "Prepare phase-compatible switches, anti-venom, food, restoration and a return route");
        add("pvm:lunar_chests", "melee",
                "Verify Perilous Moons completion and Neypotzli access",
                "Prepare for only the Moon bosses the account intends to subdue before looting",
                "Use the dungeon's verified food and potion preparation or carry an independently complete setup");
        add("pvm:mad_angel", "crush melee",
                "Verify Fallen From Grace completion and repeatable Mad Angel access in Ardeaglais",
                "Use a legal crush or verified golembane setup and review the encounter mechanics before waking the boss",
                "Carry food, restoration and an escape; quest-version completion does not prove repeatable-boss readiness");
        add("pvm:maggot_king", "hybrid",
                "Verify The Blood Moon Rises completion and post-quest Vampyrium access",
                "Treat this as high-end solo content and verify every phase-specific weapon and prayer response",
                "Prepare poison handling, food, restoration and death-cost acceptance; never infer player execution skill");
        add("pvm:shellbane_gryphon", "melee",
                "Verify Troubled Tortugans completion plus a live gryphon Slayer assignment or an eligible Elite clue",
                "Equip a tortugan shield and verify the chosen setup meets the encounter's protection mechanics",
                "Use Protect from Melee, carry food and plan access via fairy ring CJQ or a valid charter route");
        add("pvm:the_hueycoatl", "crush melee",
                "Verify Children of the Sun completion, speak to Taala, and choose a public group or paid private instance",
                "Prepare crush damage for the tail and protection-prayer responses for the projectile phases",
                "Carry food and restoration; solo possibility does not make soloing the practical recommendation");
        add("pvm:the_royal_titans", "melee, ranged and magic",
                "Verify the Asgarnia Ice Dungeon route; no quest or formal stat requirement is assumed",
                "Prepare melee as the primary style plus compatible ranged and fire/water spell responses",
                "Carry food, restoration and an escape; distinguish recommended stats from hard access");
        add("pvm:yama", "slash melee or magic",
                "Verify A Kingdom Divided completion and access through the Voice of Yama in the Chasm of Fire",
                "Attempt the inhibited fight before considering any contract; contract readiness is never inferred",
                "Prepare protection-prayer swaps, poison treatment, food, restoration and a verified demonbane or magic setup");
    }

    private void wild(String id, String style)
    {
        add("pvm:" + id, style,
                "Enable Wilderness methods explicitly and verify the selected cave/route",
                "Use only disposable risk-appropriate gear and verify the encounter's escape restrictions",
                "Carry minimal supplies and a tested escape; Hardcore accounts should receive a safer alternative");
    }

    private void add(String id, String style, String... checks)
    {
        PvmPreparationProfile value = new PvmPreparationProfile(id, style,
                Arrays.asList(checks), "Only prioritize when drops, Slayer, diary, CA, money, or an explicit collection goal justify the setup.",
                PROVENANCE);
        if (profiles.put(id, value) != null)
            throw new IllegalStateException("Duplicate PvM preparation profile: " + id);
    }
}
