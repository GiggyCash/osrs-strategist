package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Concrete method library layered on top of the early prototype catalog.
 *
 * <p>The scores intentionally represent relative Strategist preferences, not
 * claimed exact XP/hour. Exact rates vary with gear, supplies, world, route,
 * tick execution and game updates. Requirements remain CHECK_NEEDED until the
 * corresponding live readers/data prove them for the current account.</p>
 */
@Singleton
public class ExpandedTrainingMethodCatalog
{
    private final List<TrainingMethod> methods = new ArrayList<>();

    public ExpandedTrainingMethodCatalog()
    {
        addCombat();
        addPrayerAndMagic();
        addRunecraft();
        addConstruction();
        addAgility();
        addHerblore();
        addThieving();
        addCrafting();
        addFletching();
        addSlayer();
        addHunter();
        addMining();
        addSmithing();
        addFishing();
        addCooking();
        addFiremaking();
        addWoodcutting();
        addFarming();
        addSailing();
    }

    public List<TrainingMethod> methodsFor(Skill skill)
    {
        List<TrainingMethod> result = new ArrayList<>();
        for (TrainingMethod method : methods)
        {
            if (method.getSkill() == skill)
            {
                result.add(method);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private void addCombat()
    {
        add("attack_f2p_melee", Skill.ATTACK, 1, 99, "F2P melee training",
                "Use the strongest sustainable F2P melee weapon on a low-risk target; upgrade weapon tiers as Attack unlocks them.",
                7, 9, 10, AttentionLevel.LOW, 15, 2, false, false, false,
                "Suitable F2P weapon", "Food if the target can damage you");
        add("strength_f2p_melee", Skill.STRENGTH, 1, 99, "F2P Strength training",
                "Use an aggressive melee style with the strongest sustainable F2P weapon on a low-risk target.",
                8, 9, 10, AttentionLevel.LOW, 15, 2, false, false, false,
                "Suitable F2P weapon");
        add("defence_f2p_melee", Skill.DEFENCE, 1, 99, "F2P Defence training",
                "Use a defensive melee style on a low-risk sustainable target. Restricted builds are filtered before this can be recommended.",
                6, 8, 9, AttentionLevel.LOW, 15, 2, false, false, false,
                "Suitable F2P weapon");
        add("melee_sand_crabs", Skill.ATTACK, 1, 99, "Sand/rock crab melee",
                "Use a low-attention crab spot with the melee style for the combat skill you want to train. Reset aggression when needed.",
                10, 12, 15, AttentionLevel.AFK, 30, 5, true, false, false,
                "Members access", "Reachable crab area");
        add("strength_sand_crabs", Skill.STRENGTH, 1, 99, "Sand/rock crab Strength",
                "Train Strength at a low-attention crab spot with your best sustainable melee setup.",
                11, 12, 15, AttentionLevel.AFK, 30, 5, true, false, false,
                "Members access", "Reachable crab area");
        add("defence_sand_crabs", Skill.DEFENCE, 1, 99, "Sand/rock crab Defence",
                "Train Defence at a low-attention crab spot. Strategist suppresses this for builds that intentionally preserve Defence.",
                9, 11, 15, AttentionLevel.AFK, 30, 5, true, false, false,
                "Members access", "Reachable crab area");
        add("attack_slayer", Skill.ATTACK, 20, 99, "Attack through Slayer",
                "Train Attack while completing appropriate Slayer tasks so combat XP also advances Slayer, drops and unlocks.",
                13, 15, 9, AttentionLevel.MODERATE, 30, 5, true, false, false,
                "Active Slayer assignment");
        add("strength_slayer", Skill.STRENGTH, 20, 99, "Strength through Slayer",
                "Train Strength during melee Slayer tasks when that style is practical for the assigned monster.",
                14, 15, 9, AttentionLevel.MODERATE, 30, 5, true, false, false,
                "Active Slayer assignment");
        add("defence_slayer", Skill.DEFENCE, 20, 99, "Defence through Slayer",
                "Train Defence during suitable Slayer tasks when the account is not Defence-restricted.",
                12, 14, 8, AttentionLevel.MODERATE, 30, 5, true, false, false,
                "Active Slayer assignment");
        add("ranged_f2p_bows", Skill.RANGED, 1, 99, "F2P bows and arrows",
                "Use the best F2P bow and ammunition you can sustain on a safe target. Prefer safespots when useful.",
                8, 9, 10, AttentionLevel.LOW, 15, 2, false, false, false,
                "Bow", "Ammunition");
        add("ranged_crabs", Skill.RANGED, 1, 99, "AFK Ranged at crabs",
                "Use a sustainable ranged weapon at crabs for low-attention XP. Avoid expensive ammunition when a cheaper option is close in value.",
                10, 12, 15, AttentionLevel.AFK, 30, 5, true, false, false,
                "Ranged weapon", "Ammunition", "Reachable crab area");
        add("ranged_slayer", Skill.RANGED, 20, 99, "Ranged through Slayer",
                "Use Ranged on Slayer assignments where the monster, safespot and ammunition supply make it practical.",
                13, 15, 9, AttentionLevel.MODERATE, 30, 5, true, false, false,
                "Active Slayer assignment", "Sustainable ammunition");
        add("ranged_cannon", Skill.RANGED, 1, 99, "Dwarf multicannon-assisted training",
                "Use a cannon only when the account has unlocked it and can economically sustain cannonballs; favor tasks where it also improves Slayer throughput.",
                16, 11, 4, AttentionLevel.MODERATE, 30, 8, true, false, false,
                "Dwarf Cannon completion", "Cannon", "Cannonballs");
        add("ranged_chinchompas", Skill.RANGED, 45, 99, "Chinchompa burst training",
                "Use chinchompas on a verified multi-target training setup only when the account can afford or self-source the supply cost.",
                20, 9, 2, AttentionLevel.ACTIVE, 30, 10, true, false, false,
                "Chinchompas", "Verified multi-target location", "Prayer/defence sustain");
    }

    private void addPrayerAndMagic()
    {
        add("prayer_f2p_bury", Skill.PRAYER, 1, 99, "Bury useful F2P bones",
                "Bury bones obtained naturally when no better verified altar route is available.",
                4, 7, 10, AttentionLevel.LOW, 10, 0, false, false, false,
                "Bones");
        add("prayer_altar", Skill.PRAYER, 1, 99, "POH/gilded altar training",
                "Offer confirmed bones at a verified player-owned-house altar route. Do not assume a house, burner state, or bone supply.",
                16, 14, 8, AttentionLevel.ACTIVE, 20, 5, true, false, false,
                "Bones", "Verified altar route");
        add("prayer_ecto", Skill.PRAYER, 1, 99, "Ectofuntus",
                "Use the Ectofuntus when its access and processing supplies are confirmed, especially for self-sufficient accounts valuing prayer efficiency.",
                9, 12, 7, AttentionLevel.ACTIVE, 20, 8, true, false, false,
                "Ectofuntus access", "Bones", "Buckets/pots or equivalent setup");
        add("prayer_chaos_altar", Skill.PRAYER, 1, 99, "Chaos Altar",
                "Use the Wilderness Chaos Altar only when Wilderness methods are enabled and the carried-value/risk plan is acceptable.",
                18, 11, 2, AttentionLevel.ACTIVE, 15, 8, true, true, false,
                "Bones", "Wilderness risk plan");
        add("magic_f2p_combat", Skill.MAGIC, 1, 99, "F2P combat Magic",
                "Use the strongest sensible F2P combat spell that your rune supply supports; safespot where useful.",
                8, 9, 8, AttentionLevel.MODERATE, 15, 3, false, false, false,
                "Runes");
        add("magic_high_alch", Skill.MAGIC, 55, 99, "High Level Alchemy",
                "Alchemy confirmed items while travelling or doing compatible activities. Never alch protected, scarce, quest or progression items.",
                11, 13, 14, AttentionLevel.LOW, 10, 1, false, false, false,
                "55 Magic", "Nature runes", "Safe alch items");
        add("magic_burst_slayer", Skill.MAGIC, 70, 99, "Burst/barrage Slayer",
                "Use multi-target Ancient Magicks on verified burstable Slayer tasks when spellbook, runes, prayer and task setup are ready.",
                19, 15, 6, AttentionLevel.ACTIVE, 30, 8, true, false, false,
                "Ancient Magicks", "Burst-capable spell", "Runes", "Burstable Slayer task");
        add("magic_teleport_utility", Skill.MAGIC, 25, 99, "Teleport and utility Magic",
                "Let normal teleports, enchants and utility spells accumulate Magic XP while advancing other goals when raw training is not urgent.",
                6, 12, 15, AttentionLevel.LOW, 10, 0, false, false, false,
                "Required runes");
    }

    private void addRunecraft()
    {
        add("runecraft_f2p_runes", Skill.RUNECRAFT, 1, 99, "F2P altar Runecraft",
                "Craft the highest useful F2P rune whose altar/talisman route is confirmed, banking or sourcing essence according to account type.",
                7, 9, 8, AttentionLevel.MODERATE, 15, 5, false, false, false,
                "Rune essence", "Verified altar access");
        add("runecraft_lava", Skill.RUNECRAFT, 23, 99, "Lava runes",
                "Use lava runes for high-intensity Runecraft only when binding supplies, teleports and altar access are fully prepared.",
                20, 8, 2, AttentionLevel.ACTIVE, 30, 10, true, false, false,
                "23 Runecraft", "Fire altar access", "Binding necklace/imbue setup");
        add("runecraft_zmi", Skill.RUNECRAFT, 27, 99, "Ourania/ZMI altar",
                "Use the Ourania route when unlocked for lower-friction conventional Runecraft and mixed rune output.",
                13, 14, 11, AttentionLevel.MODERATE, 30, 6, true, false, false,
                "27 Runecraft", "Ourania access", "Essence");
        add("runecraft_blood", Skill.RUNECRAFT, 77, 99, "Blood runes",
                "Craft blood runes when the required route is unlocked for relaxed useful-rune production.",
                10, 14, 17, AttentionLevel.LOW, 30, 5, true, false, false,
                "77 Runecraft", "Verified blood-rune route");
        add("runecraft_soul", Skill.RUNECRAFT, 90, 99, "Soul runes",
                "Craft soul runes when unlocked for high-level low-intensity Runecraft.",
                11, 14, 16, AttentionLevel.LOW, 30, 5, true, false, false,
                "90 Runecraft", "Verified soul-rune route");
    }

    private void addConstruction()
    {
        add("construction_basic_furniture", Skill.CONSTRUCTION, 1, 32, "Basic furniture progression",
                "Build the best practical verified furniture using supplies the account already owns or can safely source.",
                10, 12, 8, AttentionLevel.ACTIVE, 15, 5, true, false, false,
                "POH access", "Planks", "Hammer", "Saw");
        add("construction_oak_larders", Skill.CONSTRUCTION, 33, 99, "Oak larders",
                "Build/remove oak larders when the account has an appropriate kitchen and sustainable oak plank supply.",
                15, 12, 6, AttentionLevel.ACTIVE, 30, 8, true, false, false,
                "33 Construction", "Kitchen", "Oak planks");
        add("construction_mahogany_tables", Skill.CONSTRUCTION, 52, 99, "Mahogany tables",
                "Use mahogany tables for high-speed Construction only when the material cost is strategically acceptable.",
                20, 8, 2, AttentionLevel.ACTIVE, 30, 8, true, false, false,
                "52 Construction", "Dining room", "Mahogany planks");
        add("construction_mahogany_homes", Skill.CONSTRUCTION, 20, 99, "Mahogany Homes contracts",
                "Run Mahogany Homes for material-efficient Construction, reward points and a less repetitive route.",
                11, 16, 15, AttentionLevel.MODERATE, 30, 8, true, false, true,
                "20 Construction", "Contract access", "Appropriate planks", "Teleport route");
    }

    private void addAgility()
    {
        add("agility_gnome", Skill.AGILITY, 1, 9, "Gnome Stronghold course",
                "Use the Gnome Stronghold course for the first Agility levels when reachable.",
                10, 11, 9, AttentionLevel.ACTIVE, 15, 3, true, false, false,
                "Gnome Stronghold access");
        add("agility_draynor", Skill.AGILITY, 10, 29, "Draynor rooftop",
                "Run Draynor rooftop for early rooftop XP and Marks of Grace.",
                12, 14, 10, AttentionLevel.ACTIVE, 20, 3, true, false, true,
                "10 Agility");
        add("agility_varrock", Skill.AGILITY, 30, 39, "Varrock rooftop",
                "Run Varrock rooftop for straightforward XP and Graceful progression.",
                12, 14, 10, AttentionLevel.ACTIVE, 20, 3, true, false, true,
                "30 Agility");
        add("agility_canifis", Skill.AGILITY, 40, 59, "Canifis rooftop",
                "Use Canifis when accessible, especially while Marks of Grace remain strategically valuable.",
                11, 15, 11, AttentionLevel.ACTIVE, 20, 4, true, false, true,
                "40 Agility", "Canifis access");
        add("agility_seers", Skill.AGILITY, 60, 89, "Seers' Village rooftop",
                "Run Seers rooftop when unlocked; diary teleports can improve the route when verified.",
                15, 15, 11, AttentionLevel.ACTIVE, 30, 4, true, false, true,
                "60 Agility", "Seers access");
        add("agility_ardy", Skill.AGILITY, 90, 99, "Ardougne rooftop",
                "Use Ardougne rooftop for high-level conventional Agility and Marks of Grace.",
                15, 15, 11, AttentionLevel.ACTIVE, 30, 4, true, false, true,
                "90 Agility");
        add("agility_sep", Skill.AGILITY, 52, 99, "Hallowed Sepulchre",
                "Use Hallowed Sepulchre for active high-skill Agility when access is verified; floor choice should match the account's level and comfort.",
                20, 15, 5, AttentionLevel.ACTIVE, 30, 8, true, false, false,
                "52 Agility", "Darkmeyer/Sepulchre access");
    }

    private void addHerblore()
    {
        add("herblore_attack_potions", Skill.HERBLORE, 3, 37, "Early useful potions",
                "Clean and process confirmed herbs into useful early potions rather than consuming scarce herbs solely for XP.",
                9, 13, 10, AttentionLevel.MODERATE, 15, 4, true, false, false,
                "Herblore unlock", "Herbs", "Secondaries", "Vials");
        add("herblore_prayer_potions", Skill.HERBLORE, 38, 62, "Prayer potion production",
                "Make prayer potions when ranarrs and snape grass are strategically available; Iron-like accounts should preserve a useful reserve.",
                10, 16, 12, AttentionLevel.MODERATE, 15, 4, true, false, false,
                "38 Herblore", "Ranarrs", "Snape grass", "Vials");
        add("herblore_super_restores", Skill.HERBLORE, 63, 76, "Super restore production",
                "Make super restores from confirmed supplies while preserving PvM reserves appropriate to the account.",
                11, 16, 12, AttentionLevel.MODERATE, 15, 4, true, false, false,
                "63 Herblore", "Snapdragon", "Red spiders' eggs");
        add("herblore_stamina", Skill.HERBLORE, 77, 99, "Stamina potion production",
                "Convert appropriate energy potions into staminas when amylase and potion reserves are confirmed.",
                12, 15, 10, AttentionLevel.MODERATE, 15, 4, true, false, false,
                "77 Herblore", "Super energy potions", "Amylase crystals");
    }

    private void addThieving()
    {
        add("thieving_f2p_pickpocket", Skill.THIEVING, 1, 99, "F2P pickpocket/stall route",
                "Use the best accessible F2P pickpocket or stall with food appropriate to your failure rate.",
                7, 9, 7, AttentionLevel.ACTIVE, 15, 2, false, false, false,
                "Reachable target");
        add("thieving_stalls", Skill.THIEVING, 5, 44, "Early stalls",
                "Use an accessible stall route for early levels, choosing useful loot when rates are close.",
                10, 12, 8, AttentionLevel.ACTIVE, 15, 2, true, false, false,
                "Accessible stall");
        add("thieving_blackjack", Skill.THIEVING, 45, 99, "Blackjacking",
                "Use blackjacking only when the required quest/access and food setup are ready. This is a high-attention efficiency option.",
                20, 8, 2, AttentionLevel.ACTIVE, 20, 5, true, false, false,
                "45 Thieving", "Pollnivneach access", "Blackjack", "Food");
        add("thieving_ardy_knights", Skill.THIEVING, 55, 99, "Ardougne knights",
                "Pickpocket Ardougne knights for repetitive low-movement training when food and failure rate are comfortable.",
                12, 15, 17, AttentionLevel.LOW, 30, 3, true, false, false,
                "55 Thieving", "Food");
        add("thieving_pyramid_plunder", Skill.THIEVING, 21, 99, "Pyramid Plunder",
                "Use Pyramid Plunder when Sophanem access is verified for varied active Thieving and collection opportunities.",
                15, 15, 9, AttentionLevel.ACTIVE, 30, 5, true, false, false,
                "Sophanem/Pyramid Plunder access");
    }

    private void addCrafting()
    {
        add("crafting_f2p_leather", Skill.CRAFTING, 1, 99, "F2P leather crafting",
                "Craft confirmed leather into the best useful F2P product available at your level.",
                7, 9, 8, AttentionLevel.MODERATE, 15, 3, false, false, false,
                "Leather", "Needle", "Thread");
        add("crafting_gems", Skill.CRAFTING, 20, 99, "Cut banked/owned gems",
                "Cut confirmed gems when they are not reserved for jewelry, bolts or another higher-value account goal.",
                13, 12, 10, AttentionLevel.LOW, 15, 2, false, false, false,
                "Uncut gems", "Chisel");
        add("crafting_glass", Skill.CRAFTING, 1, 99, "Glassblowing",
                "Process verified molten glass into the best practical product, preserving special glass needs for quests or upgrades.",
                11, 14, 15, AttentionLevel.LOW, 20, 3, true, false, false,
                "Molten glass", "Glassblowing pipe");
        add("crafting_battlestaves", Skill.CRAFTING, 54, 99, "Battlestaff crafting",
                "Attach charged orbs to battlestaves when the orb/staff supply and downstream alch/sale plan are confirmed.",
                12, 15, 11, AttentionLevel.LOW, 15, 4, true, false, false,
                "54 Crafting", "Battlestaves", "Charged orbs");
    }

    private void addFletching()
    {
        add("fletching_f2p_bows", Skill.FLETCHING, 1, 99, "F2P bows and arrows",
                "Fletch available logs into useful bows, shafts or ammunition components according to your level and supply needs.",
                8, 11, 12, AttentionLevel.LOW, 15, 1, false, false, false,
                "Knife", "Logs or ammunition supplies");
        add("fletching_bows", Skill.FLETCHING, 5, 99, "Longbow progression",
                "Fletch the best practical shortbow/longbow tier from confirmed logs, then string only when the bowstrings and product value justify it.",
                10, 13, 14, AttentionLevel.LOW, 20, 2, true, false, false,
                "Knife", "Logs");
        add("fletching_darts", Skill.FLETCHING, 10, 99, "Dart fletching",
                "Fletch darts for fast portable XP only after the account has the quest unlock and an economically sensible dart supply.",
                19, 9, 4, AttentionLevel.ACTIVE, 15, 2, true, false, false,
                "The Tourist Trap dart unlock", "Dart tips", "Feathers");
    }

    private void addSlayer()
    {
        add("slayer_safe_task", Skill.SLAYER, 1, 99, "Safe assignment progression",
                "Complete the current assignment with a master and loadout appropriate to your combat stats. Hardcore modes heavily favor low-death-risk routes.",
                12, 16, 11, AttentionLevel.MODERATE, 30, 5, true, false, false,
                "Current Slayer task");
        add("slayer_burst_tasks", Skill.SLAYER, 65, 99, "Burst/barrage assignments",
                "Prioritize verified multi-target tasks when Ancient Magicks, prayer sustain and rune supply make bursting worthwhile.",
                19, 15, 6, AttentionLevel.ACTIVE, 30, 8, true, false, false,
                "Appropriate Slayer task", "Ancient Magicks", "Runes");
        add("slayer_cannon_tasks", Skill.SLAYER, 1, 99, "Cannon-assisted assignments",
                "Use a cannon on tasks where it materially improves throughput and cannonball cost is acceptable for the account mode.",
                17, 12, 5, AttentionLevel.MODERATE, 30, 8, true, false, false,
                "Dwarf Cannon unlock", "Cannonballs", "Compatible task");
    }

    private void addHunter()
    {
        add("hunter_birds", Skill.HUNTER, 1, 28, "Bird snares",
                "Use bird snares on the best confirmed creature for early Hunter.",
                9, 11, 9, AttentionLevel.MODERATE, 15, 3, true, false, false,
                "Bird snare", "Reachable Hunter area");
        add("hunter_salamanders", Skill.HUNTER, 29, 62, "Salamanders",
                "Trap the best accessible salamander/lizard tier when equipment and area access are confirmed.",
                13, 14, 9, AttentionLevel.MODERATE, 20, 5, true, false, false,
                "Rope/net setup", "Reachable habitat");
        add("hunter_red_chins", Skill.HUNTER, 63, 99, "Red chinchompas",
                "Hunt red chinchompas for strong active Hunter XP and a useful Ranged resource when the area is available.",
                18, 15, 7, AttentionLevel.ACTIVE, 30, 5, true, false, false,
                "63 Hunter", "Box traps");
        add("hunter_black_chins", Skill.HUNTER, 73, 99, "Black chinchompas",
                "Hunt black chinchompas only when Wilderness methods are enabled and the escape/risk setup is acceptable.",
                20, 11, 2, AttentionLevel.ACTIVE, 20, 6, true, true, false,
                "73 Hunter", "Box traps", "Wilderness risk plan");
        add("hunter_herbiboar", Skill.HUNTER, 80, 99, "Herbiboar",
                "Track Herbiboar for relaxed Hunter plus herbs when Fossil Island and required tracking conditions are ready.",
                12, 16, 15, AttentionLevel.LOW, 30, 5, true, false, false,
                "80 Hunter", "Fossil Island access");
    }

    private void addMining()
    {
        add("mining_f2p_iron", Skill.MINING, 15, 99, "F2P iron ore",
                "Mine iron at a convenient F2P site for active conventional Mining, banking only when the ore is useful or valuable enough.",
                13, 12, 7, AttentionLevel.ACTIVE, 20, 3, false, false, false,
                "15 Mining", "Pickaxe");
        add("mining_granite", Skill.MINING, 45, 99, "Granite",
                "Mine granite for high-intensity Mining when desert access, waterskins/heat protection and the intended click intensity fit the session.",
                20, 10, 3, AttentionLevel.ACTIVE, 30, 8, true, false, false,
                "45 Mining", "Desert access", "Pickaxe");
        add("mining_stars", Skill.MINING, 10, 99, "Shooting Stars",
                "Mine a located star for very low-attention Mining and stardust rewards when a reachable star is known.",
                7, 14, 20, AttentionLevel.AFK, 30, 3, true, false, false,
                "Reachable Shooting Star", "Pickaxe");
        add("mining_volcanic", Skill.MINING, 50, 99, "Volcanic Mine",
                "Use Volcanic Mine only when Fossil Island access and the activity requirements/team setup are confirmed.",
                18, 15, 8, AttentionLevel.ACTIVE, 30, 10, true, false, false,
                "50 Mining", "Fossil Island", "Volcanic Mine requirements");
    }

    private void addSmithing()
    {
        add("smithing_f2p_bars", Skill.SMITHING, 1, 99, "F2P bars and items",
                "Smelt or smith confirmed F2P ore/bars into the most useful or valuable product available to your level.",
                8, 11, 9, AttentionLevel.MODERATE, 15, 4, false, false, false,
                "Ore or bars", "Furnace/anvil route");
        add("smithing_platebodies", Skill.SMITHING, 48, 99, "Platebody smithing",
                "Smith platebodies from confirmed bars for conventional fast anvil XP when the material cost is acceptable.",
                14, 11, 7, AttentionLevel.MODERATE, 20, 4, false, false, false,
                "Suitable bars", "Hammer", "Anvil");
        add("smithing_blast_furnace", Skill.SMITHING, 15, 99, "Blast Furnace",
                "Use Blast Furnace when access, ore, coal and operating-cost requirements are confirmed; choose bar type based on account needs and budget.",
                18, 15, 7, AttentionLevel.ACTIVE, 30, 8, true, false, false,
                "Blast Furnace access", "Ore/coal", "Operating GP where required");
    }

    private void addFishing()
    {
        add("fishing_f2p_fly", Skill.FISHING, 20, 99, "F2P fly fishing",
                "Fly fish trout/salmon at a convenient F2P spot for strong low-cost conventional XP.",
                12, 14, 14, AttentionLevel.LOW, 20, 2, false, false, false,
                "20 Fishing", "Fly fishing rod", "Feathers");
        add("fishing_barbarian", Skill.FISHING, 48, 99, "Barbarian Fishing",
                "Use Barbarian Fishing when the training unlock is confirmed for efficient Fishing with incidental Strength/Agility XP.",
                18, 15, 11, AttentionLevel.MODERATE, 30, 5, true, false, false,
                "Barbarian Fishing training", "Suitable rod/bait");
        add("fishing_karambwan", Skill.FISHING, 65, 99, "Karambwans",
                "Fish karambwans for low-attention useful food when quest/access, vessel and bait are ready.",
                10, 16, 18, AttentionLevel.AFK, 30, 5, true, false, false,
                "Karambwan access", "Karambwan vessel", "Bait");
        add("fishing_monkfish", Skill.FISHING, 62, 99, "Monkfish",
                "Fish monkfish for relaxed bankable food when Piscatoris access is confirmed.",
                9, 13, 17, AttentionLevel.AFK, 30, 4, true, false, false,
                "62 Fishing", "Piscatoris access", "Small fishing net");
        add("fishing_infernal_eels", Skill.FISHING, 80, 99, "Infernal eels",
                "Fish infernal eels for very low-attention training and stackable resources when Mor Ul Rek access is verified.",
                8, 13, 19, AttentionLevel.AFK, 30, 5, true, false, false,
                "80 Fishing", "Mor Ul Rek access", "Oily fishing rod");
        add("fishing_sacred_eels", Skill.FISHING, 87, 99, "Sacred eels",
                "Fish sacred eels for relaxed Fishing/Cooking value and Zulrah-scale resources when access is confirmed.",
                9, 14, 18, AttentionLevel.AFK, 30, 5, true, false, false,
                "87 Fishing", "Zul-Andra access");
    }

    private void addCooking()
    {
        add("cooking_f2p_range", Skill.COOKING, 1, 99, "F2P range cooking",
                "Cook confirmed raw food at the closest low-burn practical F2P range or fire.",
                8, 11, 13, AttentionLevel.LOW, 15, 2, false, false, false,
                "Raw food", "Range/fire access");
        add("cooking_wines", Skill.COOKING, 35, 99, "Jugs of wine",
                "Make wine for fast bankstanding Cooking only when grapes/jugs are affordable or already owned.",
                18, 10, 7, AttentionLevel.MODERATE, 20, 2, false, false, false,
                "35 Cooking", "Grapes", "Jugs of water");
        add("cooking_karambwan", Skill.COOKING, 30, 99, "Karambwan cooking",
                "Cook confirmed raw karambwans for useful food and strong XP. High-click variants should only win in efficient sessions.",
                17, 15, 11, AttentionLevel.ACTIVE, 20, 3, true, false, false,
                "Karambwan cooking unlock", "Raw karambwans");
        add("cooking_hosidius", Skill.COOKING, 1, 99, "Low-burn kitchen cooking",
                "Use a verified low-burn kitchen/range for valuable food when the access requirement is met.",
                10, 15, 16, AttentionLevel.LOW, 30, 4, true, false, false,
                "Verified low-burn range", "Raw food");
    }

    private void addFiremaking()
    {
        add("firemaking_f2p_logs", Skill.FIREMAKING, 1, 99, "F2P log burning",
                "Burn spare F2P logs in a safe line when they are not needed for Fletching or another account goal.",
                10, 10, 7, AttentionLevel.ACTIVE, 15, 1, false, false, false,
                "Logs", "Tinderbox");
        add("firemaking_wintertodt_safe", Skill.FIREMAKING, 50, 99, "Wintertodt safe setup",
                "Train at Wintertodt with warm clothing, food and a conservative damage plan. Hardcore modes require the safer preparation route.",
                14, 16, 11, AttentionLevel.MODERATE, 30, 6, true, false, true,
                "50 Firemaking", "Warm clothing", "Food", "Wintertodt access");
    }

    private void addWoodcutting()
    {
        add("woodcutting_f2p_willow", Skill.WOODCUTTING, 30, 99, "F2P willow trees",
                "Cut willows for inexpensive low-attention F2P Woodcutting when banking the logs is not strategically important.",
                10, 12, 16, AttentionLevel.LOW, 20, 2, false, false, false,
                "30 Woodcutting", "Axe");
        add("woodcutting_teak", Skill.WOODCUTTING, 35, 99, "Teak trees",
                "Cut teak for faster active Woodcutting when a good teak location is accessible and the logs are not needed elsewhere.",
                17, 13, 8, AttentionLevel.ACTIVE, 30, 4, true, false, false,
                "35 Woodcutting", "Reachable teak trees", "Axe");
        add("woodcutting_sulliuscep", Skill.WOODCUTTING, 65, 99, "Sulliuscep mushrooms",
                "Use the Sulliuscep route for strong active Woodcutting and useful Fossil Island rewards when the route is unlocked.",
                17, 15, 8, AttentionLevel.ACTIVE, 30, 6, true, false, false,
                "65 Woodcutting", "Fossil Island access", "Food/route readiness");
        add("woodcutting_redwood", Skill.WOODCUTTING, 90, 99, "Redwood trees",
                "Cut redwoods for very low-attention high-level Woodcutting when the Woodcutting Guild route is verified.",
                9, 14, 20, AttentionLevel.AFK, 30, 3, true, false, false,
                "90 Woodcutting", "Woodcutting Guild access", "Axe");
    }

    private void addFarming()
    {
        add("farming_allotments", Skill.FARMING, 1, 99, "Allotment and flower runs",
                "Use verified reachable allotment/flower patches when seeds, compost and tools are actually available.",
                9, 13, 12, AttentionLevel.LOW, 10, 5, true, false, false,
                "Reachable patch", "Seeds", "Tools", "Compost plan");
        add("farming_tree_runs", Skill.FARMING, 15, 99, "Tree runs",
                "Run only verified reachable tree/fruit-tree patches with confirmed saplings and protection/compost choices.",
                17, 17, 14, AttentionLevel.LOW, 15, 8, true, false, false,
                "Saplings", "Reachable tree patches", "Tools");
        add("farming_tithe", Skill.FARMING, 34, 99, "Tithe Farm",
                "Use Tithe Farm for active Farming XP and reward progression when access and the appropriate seed tier are confirmed.",
                16, 13, 6, AttentionLevel.ACTIVE, 30, 6, true, false, true,
                "34 Farming", "Tithe Farm access");
        add("farming_contracts", Skill.FARMING, 45, 99, "Farming contracts",
                "Use Farming Guild contracts when guild tier and contract state are observed; prioritize seed sustainability for Iron-like accounts.",
                10, 17, 14, AttentionLevel.LOW, 10, 4, true, false, false,
                "Farming Guild access", "Observed contract state");
    }

    private void addSailing()
    {
        add("sailing_relaxed", Skill.SAILING, 1, 99, "Relaxed verified Sailing activity",
                "Choose an observed unlocked Sailing activity with low setup and low attention. Strategist will not invent a port or activity that has not been verified.",
                8, 13, 17, AttentionLevel.LOW, 20, 5, true, false, false,
                "Verified Sailing port", "Verified activity unlock");
        add("sailing_efficient", Skill.SAILING, 1, 99, "Efficient verified Sailing activity",
                "Choose the strongest verified Sailing activity for the current level only after its port, requirements and route have been validated against current game data.",
                17, 14, 8, AttentionLevel.ACTIVE, 30, 8, true, false, false,
                "Verified Sailing port", "Verified activity unlock", "Required ship/resources");
    }

    private void add(String id, Skill skill, int min, int max, String name,
            String instructions, double efficient, double balanced, double relaxed,
            AttentionLevel attention, int sessionMinutes, int setupMinutes,
            boolean membersOnly, boolean wilderness, boolean progressionProtected,
            String... requirements)
    {
        List<String> requirementList = new ArrayList<>();
        Collections.addAll(requirementList, requirements);
        methods.add(new TrainingMethod(
                id, skill, min, max, name, instructions,
                efficient, balanced, relaxed, attention,
                sessionMinutes, setupMinutes, requirementList,
                requirements.length == 0
                        ? RecommendationConfidence.VERIFIED
                        : RecommendationConfidence.CHECK_NEEDED,
                membersOnly, wilderness, progressionProtected));
    }
}
