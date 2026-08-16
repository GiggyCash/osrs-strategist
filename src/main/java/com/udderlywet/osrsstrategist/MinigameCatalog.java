package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Broad minigame, skilling-boss, repeatable-activity, and D&D catalog.
 *
 * <p>Catalog membership does not mean an account can use an activity. The live
 * candidate provider still requires an observed/unlocked entry in
 * {@link MinigameSnapshot}. That deny-by-default boundary is important because
 * many activities have quest, area, combat, diary, or social requirements that
 * cannot safely be inferred from a single skill level.</p>
 *
 * <p>When a minimum level is not sufficient to prove access, the catalog uses a
 * conservative floor and relies on verified unlock state rather than inventing
 * a detailed prerequisite. Exact prerequisite modeling can then be added to a
 * dedicated evaluator without rewriting the activity identity list.</p>
 */
@Singleton
public class MinigameCatalog
{
    private final List<MinigameDefinition> definitions = new ArrayList<>();

    public MinigameCatalog()
    {
        skillingAndProduction();
        combatAndPvp();
        hybridActivities();
        diversionsAndUtilities();
        sailingActivities();
    }

    private void skillingAndProduction()
    {
        add("wintertodt", "Wintertodt", Skill.FIREMAKING, 50, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE,
                "Firemaking XP, Pyromancer, Tome of fire, supplies and Collection Log");
        add("tempoross", "Tempoross", Skill.FISHING, 35, false,
                RiskLevel.LOW, AttentionLevel.MODERATE,
                "Fishing XP, fish barrel, tackle box, Spirit angler and supplies");
        add("guardians-of-the-rift", "Guardians of the Rift", Skill.RUNECRAFT, 27, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Runecraft XP, Raiments of the Eye, abyssal needle/lantern and runes");
        add("giants-foundry", "Giants' Foundry", Skill.SMITHING, 15, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Smithing XP, coins, Smiths' Uniform and mould progression");
        add("mahogany-homes", "Mahogany Homes", Skill.CONSTRUCTION, 1, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Construction XP, Carpenter outfit and plank sack");
        add("tithe-farm", "Tithe Farm", Skill.FARMING, 34, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE,
                "Farming XP, Farmer outfit, seed box, herb sack and farming rewards");
        add("mastering-mixology", "Mastering Mixology", Skill.HERBLORE, 60, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Herblore XP, potion-storage and Herblore reward progression");
        add("motherlode-mine", "Motherlode Mine", Skill.MINING, 30, false,
                RiskLevel.NONE, AttentionLevel.LOW,
                "Prospector, coal bag, gem bag, ores and Mining XP");
        add("volcanic-mine", "Volcanic Mine", Skill.MINING, 50, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE,
                "Strong Mining XP, volcanic rewards and Collection Log progression");
        add("blast-mine", "Blast Mine", Skill.MINING, 43, false,
                RiskLevel.MEDIUM, AttentionLevel.ACTIVE,
                "Mining XP and higher-tier ores");
        add("shooting-stars", "Shooting Stars", Skill.MINING, 10, false,
                RiskLevel.NONE, AttentionLevel.AFK,
                "Very low-attention Mining XP and stardust rewards");
        add("blast-furnace", "Blast Furnace", Skill.SMITHING, 1, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Fast bar production, reduced coal usage and Smithing progression");
        add("fishing-trawler", "Fishing Trawler", Skill.FISHING, 15, false,
                RiskLevel.LOW, AttentionLevel.LOW,
                "Angler outfit and Fishing Trawler Collection Log");
        add("rogues-den", "Rogues' Den", Skill.THIEVING, 50, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE,
                "Rogue equipment for double pickpocket loot");
        add("mage-training-arena", "Mage Training Arena", Skill.MAGIC, 7, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE,
                "Mage's book, Infinity pieces, Bones to Peaches and master wand progression");
        add("hallowed-sepulchre", "Hallowed Sepulchre", Skill.AGILITY, 52, false,
                RiskLevel.MEDIUM, AttentionLevel.ACTIVE,
                "Agility XP, Hallowed marks, tools, cosmetics and valuable loot");
        add("brimhaven-agility-arena", "Brimhaven Agility Arena", Skill.AGILITY, 1, false,
                RiskLevel.LOW, AttentionLevel.ACTIVE,
                "Agility XP and arena-ticket rewards");
        add("pyramid-plunder", "Pyramid Plunder", Skill.THIEVING, 21, false,
                RiskLevel.MEDIUM, AttentionLevel.ACTIVE,
                "Thieving XP and Pharaoh's sceptre progression");
        add("sorceress-garden", "Sorceress's Garden", Skill.THIEVING, 1, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Thieving XP and Sq'irk juice rewards");
        add("stealing-artefacts", "Stealing artefacts", Skill.THIEVING, 49, false,
                RiskLevel.LOW, AttentionLevel.ACTIVE,
                "Thieving XP through Piscarilius artefact deliveries");
        add("stealing-valuables", "Stealing valuables", Skill.THIEVING, 1, false,
                RiskLevel.LOW, AttentionLevel.MODERATE,
                "Thieving progression and valuables from Varlamore content");
        add("impetuous-impulses", "Impetuous Impulses", Skill.HUNTER, 17, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Hunter XP and impling loot");
        add("archery-competition", "Archery Competition", Skill.RANGED, 1, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE,
                "Ranged XP and Archery tickets");
        add("gnome-restaurant", "Gnome Restaurant", Skill.COOKING, 29, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Cooking deliveries, travel rewards and Collection Log progression");
        add("gnome-ball", "Gnome Ball", Skill.AGILITY, 1, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE,
                "Repeatable Agility-style activity and gnomeball rewards");
        add("the-mess", "The Mess", Skill.COOKING, 1, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Cooking-oriented repeatable activity and Hosidius progression");
        add("vale-totems", "Vale Totems", null, 1, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Repeatable Varlamore skilling progression and rewards");
    }

    private void combatAndPvp()
    {
        add("pest-control", "Pest Control", null, 1, false,
                RiskLevel.LOW, AttentionLevel.MODERATE,
                "Void and Elite Void equipment");
        add("barbarian-assault", "Barbarian Assault", null, 1, false,
                RiskLevel.LOW, AttentionLevel.ACTIVE,
                "Fighter torso, role levels and Kandarin diary progression");
        add("warriors-guild", "Warriors' Guild cyclopes", null, 1, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE,
                "Defender progression up through dragon defender");
        add("soul-wars", "Soul Wars", null, 1, false,
                RiskLevel.LOW, AttentionLevel.ACTIVE,
                "Zeal, imbues and Collection Log rewards");

        // OSRS Wiki currently identifies these four as the F2P minigames.
        add("emirs-arena", "Emir's Arena", null, 1, true,
                RiskLevel.NONE, AttentionLevel.ACTIVE,
                "Safe PvP practice and arena rewards");
        add("clan-wars", "Clan Wars", null, 1, true,
                RiskLevel.NONE, AttentionLevel.ACTIVE,
                "Free-to-play team PvP and safe practice options");
        add("castle-wars", "Castle Wars", null, 1, true,
                RiskLevel.NONE, AttentionLevel.ACTIVE,
                "F2P/P2P capture-the-flag rewards and Collection Log progression");
        add("last-man-standing", "Last Man Standing", null, 1, true,
                RiskLevel.NONE, AttentionLevel.ACTIVE,
                "PvP practice and LMS reward points without risking normal gear");

        add("bounty-hunter", "Bounty Hunter", null, 1, false,
                RiskLevel.HIGH, AttentionLevel.ACTIVE,
                "Dangerous PvP target hunting and Bounty Hunter rewards");
        add("tzhaar-fight-pit", "TzHaar Fight Pit", null, 1, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE,
                "Safe competitive PvP and TzHaar progression");
        add("champions-challenge", "Champions' Challenge", null, 1, false,
                RiskLevel.MEDIUM, AttentionLevel.ACTIVE,
                "Champion scroll completion and Collection Log progression");
        add("shayzien-combat-ring", "Shayzien Combat Ring", null, 1, false,
                RiskLevel.LOW, AttentionLevel.ACTIVE,
                "Shayzien combat progression and practice");
    }

    private void hybridActivities()
    {
        add("shades-of-mortton", "Shades of Mort'ton", Skill.FIREMAKING, 20, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE,
                "Shade keys, Prayer/Firemaking progression, Zealot robes and Collection Log");
        add("tai-bwo-wannai-cleanup", "Tai Bwo Wannai Cleanup", Skill.WOODCUTTING, 1, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE,
                "Trading sticks, village favour, Woodcutting and combat rewards");
        add("the-gauntlet", "The Gauntlet", null, 1, false,
                RiskLevel.HIGH, AttentionLevel.ACTIVE,
                "Self-contained skilling/combat challenge, crystal rewards and Collection Log");
        add("trouble-brewing", "Trouble Brewing", Skill.COOKING, 40, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Pieces of eight and Trouble Brewing rewards");
        add("temple-trekking", "Temple Trekking / Burgh de Rott Ramble", null, 1, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE,
                "Lumberjack outfit and resource rewards");
        add("underwater-agility-thieving", "Underwater Agility and Thieving", null, 1, false,
                RiskLevel.LOW, AttentionLevel.ACTIVE,
                "Hybrid Agility/Thieving XP and underwater rewards");
        add("chompy-bird-hunting", "Chompy bird hunting", null, 1, false,
                RiskLevel.LOW, AttentionLevel.ACTIVE,
                "Chompy kills, hats, diary progress and Collection Log");
        add("creature-creation", "Creature Creation", null, 1, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE,
                "Tower of Life creature drops and repeatable combat/resource collection");
        add("camdozaal-vault", "Camdozaal Vault", null, 1, false,
                RiskLevel.LOW, AttentionLevel.MODERATE,
                "Below Ice Mountain skilling/combat rewards and Collection Log");
    }

    private void diversionsAndUtilities()
    {
        add("tears-of-guthix", "Tears of Guthix", null, 1, false,
                RiskLevel.NONE, AttentionLevel.LOW,
                "Periodic experience for the account's lowest eligible skill");
        add("managing-miscellania", "Managing Miscellania", null, 1, false,
                RiskLevel.NONE, AttentionLevel.LOW,
                "Kingdom resources, approval management and long-term account supplies");
        add("burthorpe-games-room", "Burthorpe Games Room", null, 1, false,
                RiskLevel.NONE, AttentionLevel.LOW,
                "Casual board-game activity and social variety");
        add("rat-pits", "Rat Pits", null, 1, false,
                RiskLevel.NONE, AttentionLevel.LOW,
                "Cat-fighting minigame progression");
        add("dorgesh-kaan-market", "Dorgesh-Kaan market trading", null, 1, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Repeatable market trading and Thieving-style rewards");
        add("keldagrim-tasks", "Keldagrim tasks", null, 1, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Repeatable Keldagrim errands and utility rewards");
        add("wise-old-man-tasks", "Wise Old Man tasks", null, 1, false,
                RiskLevel.NONE, AttentionLevel.MODERATE,
                "Repeatable errands and small utility rewards");
        add("sled-racing", "Sled Racing", null, 1, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE,
                "Optional repeatable sled activity and variety");
    }

    private void sailingActivities()
    {
        add("barracuda-trials", "Barracuda Trials", Skill.SAILING, 30, false,
                RiskLevel.MEDIUM, AttentionLevel.ACTIVE,
                "Active Sailing XP and Sailing trial progression");
        add("deep-sea-trawling", "Deep Sea Trawling", Skill.SAILING, 60, false,
                RiskLevel.MEDIUM, AttentionLevel.LOW,
                "Hybrid Sailing/Fishing progression and deep-sea rewards");
        add("port-tasks", "Port Tasks", Skill.SAILING, 1, false,
                RiskLevel.LOW, AttentionLevel.MODERATE,
                "Sailing XP, port progression and route unlocks");
        add("shipwreck-salvaging", "Shipwreck Salvaging", Skill.SAILING, 15, false,
                RiskLevel.LOW, AttentionLevel.MODERATE,
                "Sailing XP and salvaged resources");
        add("sea-charting", "Sea Charting", Skill.SAILING, 1, false,
                RiskLevel.LOW, AttentionLevel.MODERATE,
                "Exploration-oriented Sailing XP and chart progression");
    }

    public List<MinigameDefinition> all()
    {
        return Collections.unmodifiableList(definitions);
    }

    public MinigameDefinition byId(String id)
    {
        if (id == null) return null;
        for (MinigameDefinition definition : definitions)
            if (id.equals(definition.getId())) return definition;
        return null;
    }

    private void add(String id, String name, Skill skill, int level,
            boolean f2p, RiskLevel risk, AttentionLevel attention,
            String rewards)
    {
        definitions.add(new MinigameDefinition(
                id, name, skill, level, f2p, risk, attention,
                allModes(), rewards));
    }

    private static EnumSet<AccountMode> allModes()
    {
        return EnumSet.of(AccountMode.MAIN, AccountMode.IRONMAN,
                AccountMode.ULTIMATE_IRONMAN, AccountMode.HARDCORE_IRONMAN,
                AccountMode.GROUP_IRONMAN, AccountMode.HARDCORE_GROUP_IRONMAN,
                AccountMode.UNRANKED_GROUP_IRONMAN);
    }
}
