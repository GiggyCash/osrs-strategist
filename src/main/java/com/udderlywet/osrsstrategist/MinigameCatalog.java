package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Broad minigame/skilling-boss progression catalog. */
@Singleton
public class MinigameCatalog
{
    private final List<MinigameDefinition> definitions = new ArrayList<>();

    public MinigameCatalog()
    {
        add("wintertodt", "Wintertodt", Skill.FIREMAKING, 50, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE, allModes(),
                "Firemaking XP, Pyromancer, Tome of fire, supplies and Collection Log");
        add("tempoross", "Tempoross", Skill.FISHING, 35, false,
                RiskLevel.LOW, AttentionLevel.MODERATE, allModes(),
                "Fishing XP, fish barrel, tackle box, Spirit angler and supplies");
        add("guardians-of-the-rift", "Guardians of the Rift", Skill.RUNECRAFT, 27, false,
                RiskLevel.NONE, AttentionLevel.MODERATE, allModes(),
                "Runecraft XP, Raiments of the Eye, abyssal needle/lantern and runes");
        add("giants-foundry", "Giants' Foundry", Skill.SMITHING, 15, false,
                RiskLevel.NONE, AttentionLevel.MODERATE, allModes(),
                "Smithing XP, coins, Smiths' Uniform and mould progression");
        add("mahogany-homes", "Mahogany Homes", Skill.CONSTRUCTION, 1, false,
                RiskLevel.NONE, AttentionLevel.MODERATE, allModes(),
                "Construction XP, Carpenter outfit and plank sack");
        add("tithe-farm", "Tithe Farm", Skill.FARMING, 34, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE, allModes(),
                "Farming XP, Farmer outfit, seed box, herb sack and farming rewards");
        add("pest-control", "Pest Control", null, 1, false,
                RiskLevel.LOW, AttentionLevel.MODERATE, allModes(),
                "Void and Elite Void equipment");
        add("barbarian-assault", "Barbarian Assault", null, 1, false,
                RiskLevel.LOW, AttentionLevel.ACTIVE, allModes(),
                "Fighter torso, role levels and Kandarin diary progression");
        add("fishing-trawler", "Fishing Trawler", Skill.FISHING, 15, false,
                RiskLevel.LOW, AttentionLevel.LOW, allModes(),
                "Angler outfit and Fishing Trawler Collection Log");
        add("rogues-den", "Rogues' Den", Skill.THIEVING, 50, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE, allModes(),
                "Rogue equipment for double pickpocket loot");
        add("mage-training-arena", "Mage Training Arena", Skill.MAGIC, 7, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE, allModes(),
                "Mage's book, Infinity pieces, Bones to Peaches and master wand progression");
        add("volcanic-mine", "Volcanic Mine", Skill.MINING, 50, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE, allModes(),
                "Strong Mining XP, volcanic mine rewards and collection progress");
        add("blast-mine", "Blast Mine", Skill.MINING, 43, false,
                RiskLevel.MEDIUM, AttentionLevel.ACTIVE, allModes(),
                "Mining XP and higher-tier ores");
        add("hallowed-sepulchre", "Hallowed Sepulchre", Skill.AGILITY, 52, false,
                RiskLevel.MEDIUM, AttentionLevel.ACTIVE, allModes(),
                "Agility XP, Hallowed marks, graceful recolour/tools and valuable loot");
        add("pyramid-plunder", "Pyramid Plunder", Skill.THIEVING, 21, false,
                RiskLevel.MEDIUM, AttentionLevel.ACTIVE, allModes(),
                "Thieving XP and Pharaoh's sceptre progression");
        add("soul-wars", "Soul Wars", null, 1, false,
                RiskLevel.LOW, AttentionLevel.ACTIVE, allModes(),
                "Zeal, imbues and collection-log rewards");
        add("last-man-standing", "Last Man Standing", null, 1, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE, allModes(),
                "PvP practice and LMS reward points without risking normal gear");
        add("castle-wars", "Castle Wars", null, 1, true,
                RiskLevel.NONE, AttentionLevel.ACTIVE, allModes(),
                "F2P/P2P minigame rewards and Collection Log");
        add("trouble-brewing", "Trouble Brewing", Skill.COOKING, 40, false,
                RiskLevel.NONE, AttentionLevel.MODERATE, allModes(),
                "Pieces of eight and Trouble Brewing rewards");
        add("shades-of-mortton", "Shades of Mort'ton", Skill.FIREMAKING, 20, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE, allModes(),
                "Shade keys, prayer/firemaking progression and collection rewards");
        add("brimhaven-agility-arena", "Brimhaven Agility Arena", Skill.AGILITY, 1, false,
                RiskLevel.LOW, AttentionLevel.ACTIVE, allModes(),
                "Agility XP, tickets and graceful recolour rewards");
        add("gnome-restaurant", "Gnome Restaurant", Skill.COOKING, 29, false,
                RiskLevel.NONE, AttentionLevel.MODERATE, allModes(),
                "Cooking deliveries, useful travel rewards and Collection Log");
        add("temple-trekking", "Temple Trekking / Burgh de Rott Ramble", null, 1, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE, allModes(),
                "Lumberjack outfit and resource rewards");
        add("warriors-guild", "Warriors' Guild cyclopes", null, 1, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE, allModes(),
                "Dragon defender progression");
        add("motherlode-mine", "Motherlode Mine", Skill.MINING, 30, false,
                RiskLevel.NONE, AttentionLevel.LOW, allModes(),
                "Prospector, coal bag, gem bag, ores and Mining XP");
        add("barracuda-trials", "Barracuda Trials", Skill.SAILING, 30, false,
                RiskLevel.MEDIUM, AttentionLevel.ACTIVE, allModes(),
                "Active Sailing XP and Sailing trial progression");
        add("deep-sea-trawling", "Deep Sea Trawling", Skill.SAILING, 60, false,
                RiskLevel.MEDIUM, AttentionLevel.LOW, allModes(),
                "Hybrid Sailing/Fishing progression and deep-sea rewards");
        add("nightmare-zone", "Nightmare Zone", null, 1, false,
                RiskLevel.LOW, AttentionLevel.LOW, allModes(),
                "Combat training, imbues and points after the required quest bosses are unlocked");
        add("blast-furnace", "Blast Furnace", Skill.SMITHING, 1, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE, allModes(),
                "Bar processing, Smithing XP, coal bag value and Blast Furnace shop access");
        add("sorceresss-garden", "Sorceress's Garden", Skill.THIEVING, 1, false,
                RiskLevel.NONE, AttentionLevel.MODERATE, allModes(),
                "Thieving XP, sq'irk juice and seasonal garden progression");
        add("tai-bwo-wannai-cleanup", "Tai Bwo Wannai Cleanup", null, 1, false,
                RiskLevel.MEDIUM, AttentionLevel.MODERATE, allModes(),
                "Trading sticks, village favour, gout tuber and Karamja progression");
        add("aerial-fishing", "Aerial Fishing", Skill.FISHING, 43, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE, allModes(),
                "Fishing and Hunter XP, Molch pearls and pearl fishing equipment");
        add("drift-net-fishing", "Drift Net Fishing", Skill.FISHING, 47, false,
                RiskLevel.LOW, AttentionLevel.ACTIVE, allModes(),
                "Combined Fishing and Hunter XP after Fossil Island underwater access");
        add("chompy-hunting", "Chompy bird hunting", Skill.RANGED, 30, false,
                RiskLevel.LOW, AttentionLevel.MODERATE, allModes(),
                "Chompy kill progression, ogre hats and Western Provinces diary value");
        add("gnome-ball", "Gnome Ball", null, 1, false,
                RiskLevel.NONE, AttentionLevel.ACTIVE, allModes(),
                "Agility experience and gnomeball activity progress");
        add("stealing-artefacts", "Stealing artefacts", Skill.THIEVING, 49, false,
                RiskLevel.LOW, AttentionLevel.ACTIVE, allModes(),
                "Thieving XP through Port Piscarilius artefact deliveries");
        add("underwater-agility-thieving", "Underwater Agility and Thieving",
                Skill.AGILITY, 48, false, RiskLevel.MEDIUM,
                AttentionLevel.ACTIVE, allModes(),
                "Combined Agility and Thieving XP in Fossil Island's underwater area");
        add("shooting-stars", "Shooting Stars", Skill.MINING, 10, true,
                RiskLevel.NONE, AttentionLevel.LOW, allModes(),
                "Mining XP, stardust, celestial ring progression and gem rewards");
        add("forestry", "Forestry", Skill.WOODCUTTING, 1, true,
                RiskLevel.NONE, AttentionLevel.MODERATE, allModes(),
                "Woodcutting group events, anima-infused bark and Forestry rewards");
        add("champions-challenge", "Champions' Challenge", null, 1, false,
                RiskLevel.MEDIUM, AttentionLevel.ACTIVE, allModes(),
                "Use an observed champion's scroll for Slayer and Hitpoints XP, banners and champion's cape progression; each champion has distinct combat restrictions");
        add("rat-pits", "Rat Pits", null, 1, false,
                RiskLevel.HIGH, AttentionLevel.ACTIVE, EnumSet.of(AccountMode.MAIN),
                "Optional cat-growth and coin-betting activity after the relevant Ratcatchers pit is unlocked; protect the cat from death");
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
            EnumSet<AccountMode> modes, String rewards)
    {
        definitions.add(new MinigameDefinition(id, name, skill, level,
                f2p, risk, attention, modes, rewards));
    }

    private static EnumSet<AccountMode> allModes()
    {
        return EnumSet.of(AccountMode.MAIN, AccountMode.IRONMAN,
                AccountMode.ULTIMATE_IRONMAN, AccountMode.HARDCORE_IRONMAN,
                AccountMode.GROUP_IRONMAN, AccountMode.HARDCORE_GROUP_IRONMAN,
                AccountMode.UNRANKED_GROUP_IRONMAN);
    }
}
