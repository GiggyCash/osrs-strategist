package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Curated real-world training methods layered on top of RuneLite/account state.
 *
 * <p>The intent is not to reduce a skill to one optimal route. Each skill keeps
 * multiple efficiency, budget, relaxed, AFK, F2P/P2P, and account-mode-friendly
 * options so the selector can adapt to the player rather than forcing a single
 * max-efficiency guide.</p>
 */
@Singleton
public class ExpandedTrainingMethodCatalog
{
    public static final String PROVENANCE =
            "RuneLite 1.12.35 skill calculators plus maintained current-live strategy audit";
    public static final String AUDITED_THROUGH = "2026-08-25";

    private final Map<Skill, List<CuratedTrainingMethod>> methods =
            new EnumMap<>(Skill.class);

    public ExpandedTrainingMethodCatalog()
    {
        for (Skill skill : Skill.values()) methods.put(skill, new ArrayList<>());
        combat();
        gathering();
        production();
        utility();
        sailing();
    }

    public List<CuratedTrainingMethod> methodsFor(Skill skill)
    {
        List<CuratedTrainingMethod> list = methods.get(skill);
        return list == null ? Collections.emptyList()
                : Collections.unmodifiableList(list);
    }

    private void combat()
    {
        for (Skill skill : Arrays.asList(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE))
        {
            add(skill, id(skill, "f2p_low"), 1, 35,
                    "Low-level F2P melee", "Fight low-defence F2P monsters with the best weapon available for the chosen style.",
                    TrainingIntensity.BALANCED, MethodCostTier.FREE, RiskLevel.NONE,
                    true, true, true, true, false, AttentionLevel.MODERATE, 10, 2);
            add(skill, id(skill, "f2p_giants"), 30, 99,
                    "F2P giants", "Train on hill or moss giants when you want simple combat plus useful drops and Prayer supplies.",
                    TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                    true, true, true, true, false, AttentionLevel.LOW, 20, 3);
            add(skill, id(skill, "crabs"), 20, 99,
                    "Crab training", "Use sand, ammonite, or another unlocked crab area for low-attention melee experience.",
                    TrainingIntensity.AFK, MethodCostTier.VERY_LOW, RiskLevel.NONE,
                    false, true, true, true, false, AttentionLevel.AFK, 20, 4,
                    "Unlocked crab area");
            add(skill, id(skill, "slayer"), 30, 99,
                    "Train through Slayer", "Use the requested melee style during suitable Slayer tasks so combat and Slayer progress together.",
                    TrainingIntensity.BALANCED, MethodCostTier.LOW, RiskLevel.MEDIUM,
                    false, true, true, true, false, AttentionLevel.MODERATE, 30, 5,
                    "Current Slayer task suitable for melee");
            add(skill, id(skill, "scurrius"), 40, 99,
                    "Scurrius", "Use Scurrius as active combat practice when the account has the supplies and encounter access for it.",
                    TrainingIntensity.EFFICIENT, MethodCostTier.LOW, RiskLevel.MEDIUM,
                    false, true, true, true, false, AttentionLevel.ACTIVE, 20, 5,
                    "Scurrius readiness and supplies");
            add(skill, id(skill, "nmz"), 70, 99,
                    "Nightmare Zone", "Use an unlocked Nightmare Zone setup for long, low-attention melee sessions.",
                    TrainingIntensity.AFK, MethodCostTier.LOW, RiskLevel.NONE,
                    false, false, false, true, false, AttentionLevel.AFK, 45, 8,
                    "Nightmare Zone quest bosses unlocked");
        }

        add(Skill.RANGED, "ranged_f2p_bows", 1, 99,
                "F2P bows and safespots", "Use the best available F2P bow/ammunition and safespot appropriate monsters where practical.",
                TrainingIntensity.BALANCED, MethodCostTier.LOW, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.MODERATE, 15, 3);
        add(Skill.RANGED, "ranged_f2p_ogress", 40, 99,
                "Safespot ogresses", "Safespot ogresses for a relaxed F2P Ranged method with alchable drops.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.LOW, 20, 3,
                "Corsair Cove access");
        add(Skill.RANGED, "ranged_crabs", 20, 99,
                "Ranged at crabs", "Use an unlocked crab area for low-attention Ranged experience.",
                TrainingIntensity.AFK, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, false, true, true, false, AttentionLevel.AFK, 20, 4,
                "Unlocked crab area");
        add(Skill.RANGED, "ranged_cannon_slayer", 1, 99,
                "Cannon Slayer tasks", "Use a dwarf multicannon on tasks where it is allowed and strategically worth the ammunition cost.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.MEDIUM,
                false, false, false, true, false, AttentionLevel.MODERATE, 30, 8,
                "Dwarf Cannon", "Cannon-eligible Slayer task", "Cannonball supply");
        add(Skill.RANGED, "ranged_chinning", 45, 99,
                "Chinchompas", "Use chinchompas in a verified multi-target training location for very fast active Ranged experience.",
                TrainingIntensity.SWEATY, MethodCostTier.VERY_HIGH, RiskLevel.MEDIUM,
                false, false, false, false, false, AttentionLevel.ACTIVE, 30, 10,
                "Multi-target training location", "Large chinchompa supply");
        add(Skill.RANGED, "ranged_slayer", 40, 99,
                "Ranged through Slayer", "Range suitable Slayer tasks to combine Ranged, Slayer, drops, and account progression.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.MODERATE, 30, 5,
                "Current Slayer task suitable for Ranged");

        add(Skill.PRAYER, "prayer_f2p_bones", 1, 99,
                "Bury F2P bones", "Bury bones obtained while training or killing giants when playing F2P or conserving resources.",
                TrainingIntensity.RELAXED, MethodCostTier.FREE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.LOW, 5, 0);
        add(Skill.PRAYER, "prayer_gilded_altar", 1, 99,
                "Gilded altar", "Offer bones at a lit gilded altar when a safe house route and bone supply are available.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.NONE,
                false, false, false, true, false, AttentionLevel.ACTIVE, 20, 8,
                "Bone supply", "Lit gilded altar access");
        add(Skill.PRAYER, "prayer_ensouled_heads", 16, 99,
                "Ensouled heads", "Reanimate and defeat ensouled creatures for a self-source-friendly Prayer route.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 6,
                "Arceuus reanimation access", "Ensouled head supply");
        add(Skill.PRAYER, "prayer_blessed_shards", 1, 99,
                "Blessed bone shards", "Process and offer blessed bone shards when the Varlamore route and supplies are verified.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 7,
                "Blessed bone shard route and supplies");
        add(Skill.PRAYER, "prayer_ectofuntus", 1, 99,
                "Ectofuntus", "Grind bones and offer bonemeal with slime at the Ectofuntus when conserving bones matters more than speed.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 7,
                "Ghosts Ahoy or verified Port Phasmatys route", "Bone, pot and bucket-of-slime supply");
        add(Skill.PRAYER, "prayer_bonecrusher_passive", 1, 99,
                "Bonecrusher during combat", "Carry a charged bonecrusher during suitable combat or Slayer when passive Prayer XP and inventory saving outweigh the charge cost.",
                TrainingIntensity.RELAXED, MethodCostTier.LOW, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.LOW, 20, 4,
                "Morytania hard diary reward", "Bonecrusher charges", "Suitable combat activity");
        add(Skill.PRAYER, "prayer_chaos_altar", 1, 99,
                "Chaos altar", "Offer bones at the Wilderness Chaos Altar only when Wilderness risk is explicitly enabled and accepted.",
                TrainingIntensity.EFFICIENT, MethodCostTier.MODERATE, RiskLevel.HIGH,
                false, true, true, false, true, AttentionLevel.ACTIVE, 15, 8,
                "Bone supply", "Wilderness risk accepted");

        add(Skill.MAGIC, "magic_f2p_combat", 1, 34,
                "Wind Strike on monks", "Edgeville Monastery, west of Edgeville: cast Wind Strike on monks, ask a monk to heal you when needed, and repeat.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.LOW,
                true, true, true, true, false, AttentionLevel.MODERATE, 15, 3,
                "Air and mind rune supply");
        add(Skill.MAGIC, "magic_f2p_curse", 19, 99,
                "F2P curse casting", "Use curse-style utility casting for lower-cost F2P Magic experience when appropriate.",
                TrainingIntensity.RELAXED, MethodCostTier.LOW, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.MODERATE, 15, 3,
                "Verified Curse splash target and equipment setup",
                "Earth, water, and body rune supply");
        add(Skill.MAGIC, "magic_f2p_fire_bolt", 35, 58,
                "Fire Bolt at Wizards' Tower", "Top floor of Wizards' Tower: cast Fire Bolt through the cage at the lesser demon, wait for it to respawn, and repeat.",
                TrainingIntensity.BALANCED, MethodCostTier.HIGH, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.MODERATE, 20, 4,
                "Air, fire, and chaos rune supply");
        add(Skill.MAGIC, "magic_f2p_fire_blast", 59, 99,
                "Fire Blast at Wizards' Tower", "Top floor of Wizards' Tower: cast Fire Blast through the cage at the lesser demon, wait for it to respawn, and repeat.",
                TrainingIntensity.BALANCED, MethodCostTier.HIGH, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.MODERATE, 20, 4,
                "Air, fire, and death rune supply");
        add(Skill.MAGIC, "magic_high_alch", 55, 99,
                "High Level Alchemy", "High-alch verified items during movement or downtime, accounting for item value before consuming them.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.LOW, 10, 2,
                "Nature rune supply", "Safe alch item list");
        add(Skill.MAGIC, "magic_bursting", 70, 93,
                "Ancient burst training", "Burst verified multi-target monsters when spellbook, runes, and supplies are ready.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.MEDIUM,
                false, false, false, true, false, AttentionLevel.ACTIVE, 30, 10,
                "Ancient Magicks", "Multi-target location", "Rune and prayer supply");
        add(Skill.MAGIC, "magic_barraging", 94, 99,
                "Ice Barrage training", "Barrage verified multi-target monsters for very high Magic experience when cost is acceptable.",
                TrainingIntensity.SWEATY, MethodCostTier.VERY_HIGH, RiskLevel.MEDIUM,
                false, false, false, true, false, AttentionLevel.ACTIVE, 30, 10,
                "Ancient Magicks", "Multi-target location", "Large rune supply");
        add(Skill.MAGIC, "magic_slayer", 65, 99,
                "Magic through Slayer", "Use burst/barrage or conventional Magic on suitable Slayer tasks for multi-skill progress.",
                TrainingIntensity.BALANCED, MethodCostTier.HIGH, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.MODERATE, 30, 6,
                "Suitable Slayer task");
        add(Skill.MAGIC, "magic_lunar_utility", 65, 99,
                "Lunar utility spells", "Use an economically sensible Lunar utility spell such as Bake Pie, String Jewellery, or Plank Make when its output advances another account goal.",
                TrainingIntensity.RELAXED, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 20, 5,
                "Lunar Diplomacy and Lunar spellbook", "Runes and matching production inputs");

        add(Skill.HITPOINTS, "hitpoints_combat", 1, 99,
                "Train through combat", "Let Hitpoints rise naturally while training combat skills or completing PvM progression.",
                TrainingIntensity.BALANCED, MethodCostTier.FREE, RiskLevel.LOW,
                true, true, true, true, false, AttentionLevel.MODERATE, 15, 0);
        add(Skill.HITPOINTS, "hitpoints_slayer", 10, 99,
                "Train through Slayer", "Gain Hitpoints passively while doing safe, account-appropriate Slayer tasks.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.MODERATE, 30, 3);
    }

    private void gathering()
    {
        add(Skill.MINING, "mining_lumbridge_copper", 1, 14,
                "Lumbridge copper", "East Lumbridge Swamp mine: mine copper, drop the ore when full, and repeat.",
                TrainingIntensity.BALANCED, MethodCostTier.FREE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.MODERATE, 15, 2);
        add(Skill.MINING, "mining_f2p_iron", 15, 99,
                "Power-mine iron", "Varrock East mine, southeast of Varrock: mine iron, drop the ore when full, and repeat.",
                TrainingIntensity.EFFICIENT, MethodCostTier.FREE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.ACTIVE, 20, 2);
        add(Skill.MINING, "mining_mlm", 30, 99,
                "Motherlode Mine", "Mine pay-dirt for a relaxed mix of Mining experience, ores, nuggets, and Prospector progression.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 30, 3);
        add(Skill.MINING, "mining_stars", 10, 99,
                "Shooting Stars", "Mine discovered stars for very low-attention Mining and stardust rewards.",
                TrainingIntensity.AFK, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.AFK, 20, 3,
                "Reachable Shooting Star");
        add(Skill.MINING, "mining_granite_3t", 45, 99,
                "3-tick granite", "Use tick manipulation on granite for high Mining XP when a sweaty session is desired.",
                TrainingIntensity.SWEATY, MethodCostTier.VERY_LOW, RiskLevel.NONE,
                false, true, false, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Desert heat protection and tick-manipulation supplies");
        add(Skill.MINING, "mining_calcified", 41, 99,
                "Calcified rocks", "Mine calcified rocks for a low-attention Varlamore method with Prayer-related rewards.",
                TrainingIntensity.AFK, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.AFK, 20, 4,
                "Calcified rocks access");
        add(Skill.MINING, "mining_gem_rocks", 40, 99,
                "Gem rocks", "Mine gem rocks when gems and profit are useful alongside solid Mining experience.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 4,
                "Gem rock access");
        add(Skill.MINING, "mining_volcanic", 50, 99,
                "Volcanic Mine", "Run Volcanic Mine when team/access requirements are met for strong Mining experience with moderate attention.",
                TrainingIntensity.EFFICIENT, MethodCostTier.LOW, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.MODERATE, 30, 8,
                "Volcanic Mine access and team/readiness");
        add(Skill.MINING, "mining_blast_mine", 43, 99,
                "Blast Mine", "Use Blast Mine when ore rewards are valuable and the active explosive loop fits the session.",
                TrainingIntensity.EFFICIENT, MethodCostTier.LOW, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 6,
                "Blast Mine access and dynamite");
        add(Skill.MINING, "mining_sandstone", 35, 99,
                "Sandstone", "Mine sandstone in the Kharidian Desert when active Mining XP and future sandstone/grinder resources are both useful.",
                TrainingIntensity.EFFICIENT, MethodCostTier.FREE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Desert heat protection", "Quarry transport and inventory plan");
        add(Skill.MINING, "mining_amethyst", 92, 99,
                "Amethyst", "Mine amethyst for low-attention Mining and valuable high-level ammunition materials.",
                TrainingIntensity.AFK, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.AFK, 30, 4,
                "Mining Guild amethyst area access");

        add(Skill.FISHING, "fishing_f2p_fly", 20, 99,
                "Fly fishing", "Barbarian Village fishing spots: catch trout and salmon, drop the fish when full, and repeat.",
                TrainingIntensity.BALANCED, MethodCostTier.VERY_LOW, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.MODERATE, 20, 2);
        add(Skill.FISHING, "fishing_barbarian", 48, 99,
                "Barbarian fishing", "Barbarian fish for strong Fishing XP plus passive Agility and Strength experience.",
                TrainingIntensity.EFFICIENT, MethodCostTier.VERY_LOW, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 30, 4,
                "Barbarian Fishing training");
        add(Skill.FISHING, "fishing_3t_barb", 48, 99,
                "3-tick Barbarian fishing", "Use tick manipulation at Barbarian Fishing for high active Fishing experience.",
                TrainingIntensity.SWEATY, MethodCostTier.VERY_LOW, RiskLevel.NONE,
                false, true, false, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Barbarian Fishing", "Tick-manipulation supplies");
        add(Skill.FISHING, "fishing_karambwan", 65, 99,
                "Karambwans", "Fish karambwans for long AFK intervals and a valuable food supply.",
                TrainingIntensity.AFK, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.AFK, 30, 6,
                "Karambwan fishing access", "Karambwanji supply");
        add(Skill.FISHING, "fishing_tempoross", 35, 99,
                "Tempoross", "Subdue Tempoross for Fishing experience, uniques, and useful account supplies.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 5,
                "Tempoross access");
        add(Skill.FISHING, "fishing_minnows", 82, 99,
                "Minnows", "Fish minnows when shark-equivalent food supply is more important than maximum AFK time.",
                TrainingIntensity.EFFICIENT, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 4,
                "Fishing Guild minnow platform access");
        add(Skill.FISHING, "fishing_anglers", 82, 99,
                "Anglerfish", "Fish anglerfish for very relaxed experience and high-value food.",
                TrainingIntensity.AFK, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.AFK, 30, 5,
                "Piscarilius anglerfish access");
        add(Skill.FISHING, "fishing_dark_crabs", 85, 99,
                "Dark crabs", "Fish dark crabs only when Wilderness risk is explicitly accepted.",
                TrainingIntensity.AFK, MethodCostTier.PROFITABLE, RiskLevel.HIGH,
                false, true, false, false, true, AttentionLevel.LOW, 20, 8,
                "Wilderness resource area access and risk accepted");
        add(Skill.FISHING, "fishing_aerial", 43, 99,
                "Aerial fishing", "Use aerial fishing when combined Fishing/Hunter XP and Molch pearl rewards justify the high attention.",
                TrainingIntensity.SWEATY, MethodCostTier.LOW, RiskLevel.NONE,
                false, true, false, true, false, AttentionLevel.ACTIVE, 20, 5,
                "35 Hunter", "Lake Molch access", "Cormorant glove setup");
        add(Skill.FISHING, "fishing_drift_net", 47, 99,
                "Drift net fishing", "Run drift nets for strong combined Fishing and Hunter progress when the net cost and underwater setup are worthwhile.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.LOW,
                false, true, false, true, false, AttentionLevel.ACTIVE, 20, 7,
                "44 Hunter", "Fossil Island underwater access", "Drift net supply");
        add(Skill.FISHING, "fishing_infernal_eels", 80, 99,
                "Infernal eels", "Fish and process infernal eels for a relaxed, bank-light route with Tokkul and onyx-bolt-tip rewards.",
                TrainingIntensity.AFK, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.AFK, 30, 6,
                "Mor Ul Rek access", "Oily fishing rod and bait", "Hammer");
        add(Skill.FISHING, "fishing_sacred_eels", 87, 99,
                "Sacred eels", "Fish and process sacred eels when Zulrah scales and low-attention profit are strategically useful.",
                TrainingIntensity.AFK, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.AFK, 30, 6,
                "Zul-Andra access", "Fishing rod, bait and knife");

        add(Skill.FISHING, "fishing_lumbridge_shrimps", 1, 19,
                "Lumbridge shrimp", "Lumbridge Swamp fishing spots beside the Fishing tutor: net shrimp, drop the catch when full, and repeat.",
                TrainingIntensity.RELAXED, MethodCostTier.FREE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.LOW, 15, 2);

        add(Skill.WOODCUTTING, "woodcutting_lumbridge_trees", 1, 14,
                "Lumbridge trees", "Trees west of Lumbridge Castle: cut regular logs, drop them when full, and repeat.",
                TrainingIntensity.RELAXED, MethodCostTier.FREE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.LOW, 15, 2);
        add(Skill.WOODCUTTING, "woodcutting_draynor_oaks", 15, 29,
                "Draynor oaks", "Oak trees beside Draynor Village bank: cut oaks, bank the logs, and repeat.",
                TrainingIntensity.RELAXED, MethodCostTier.FREE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.LOW, 15, 2);
        add(Skill.WOODCUTTING, "woodcutting_f2p_willows", 30, 99,
                "F2P willows", "Willow trees beside Draynor Village bank: cut willows, bank the logs, and repeat.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.LOW, 20, 2);
        add(Skill.WOODCUTTING, "woodcutting_teaks", 35, 99,
                "Teak trees", "Cut teak trees for strong conventional Woodcutting experience.",
                TrainingIntensity.EFFICIENT, MethodCostTier.FREE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 4,
                "Reachable teak trees");
        add(Skill.WOODCUTTING, "woodcutting_tick_teaks", 35, 99,
                "Tick-manipulated teaks", "Use tick manipulation on teak trees when maximizing active Woodcutting experience.",
                TrainingIntensity.SWEATY, MethodCostTier.VERY_LOW, RiskLevel.NONE,
                false, true, false, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Tick-manipulation supplies");
        add(Skill.WOODCUTTING, "woodcutting_forestry", 15, 99,
                "Forestry", "Join Forestry events while cutting appropriate trees for social, varied Woodcutting progression.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 3,
                "Forestry-enabled tree location");
        add(Skill.WOODCUTTING, "woodcutting_sulliusceps", 65, 99,
                "Sulliusceps", "Cut sulliusceps for strong experience and fossil progression when Fossil Island access is ready.",
                TrainingIntensity.EFFICIENT, MethodCostTier.PROFITABLE, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 6,
                "Fossil Island and mushroom transport access");
        add(Skill.WOODCUTTING, "woodcutting_redwoods", 90, 99,
                "Redwoods", "Cut redwoods for one of the most relaxed high-level Woodcutting methods.",
                TrainingIntensity.AFK, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.AFK, 30, 3,
                "Woodcutting Guild redwood access");

        add(Skill.HUNTER, "hunter_birdhouses", 5, 99,
                "Birdhouse runs", "Complete birdhouse runs when passive nests and low-time progress fit the account; live August 2026 XP is lower by tier, so do not treat them as the default fastest Hunter route.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 5, 5,
                "Birdhouse access and supplies");
        add(Skill.HUNTER, "hunter_salamanders", 29, 99,
                "Salamanders", "Catch the best practical salamander for active Hunter experience.",
                TrainingIntensity.EFFICIENT, MethodCostTier.VERY_LOW, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 4,
                "Reachable salamander habitat");
        add(Skill.HUNTER, "hunter_chins", 53, 99,
                "Chinchompas", "Catch chinchompas when you value tradable profit or a future Ranged supply.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 4);
        add(Skill.HUNTER, "hunter_red_chins", 63, 99,
                "Red chinchompas", "Catch red chinchompas for high Hunter experience and valuable Ranged ammunition.",
                TrainingIntensity.EFFICIENT, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Red chinchompa area access");
        add(Skill.HUNTER, "hunter_rumours", 46, 99,
                "Hunter Rumours", "Complete Hunter Rumours for varied training, rewards, and account progression.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.MODERATE, 25, 5,
                "Hunter Guild rumour access");
        add(Skill.HUNTER, "hunter_herbiboar", 80, 99,
                "Herbiboar", "Track herbiboar for relaxed Hunter experience plus herbs and Herblore supplies.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 25, 5,
                "Fossil Island herbiboar access");
        add(Skill.HUNTER, "hunter_bird_traps", 1, 28,
                "Feldip bird snares", "Feldip Hunter area south of Yanille: catch crimson swifts, switching to tropical wagtails at 19 Hunter; reset collapsed traps and repeat.",
                TrainingIntensity.BALANCED, MethodCostTier.VERY_LOW, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 15, 3,
                "Bird snare");
        add(Skill.HUNTER, "hunter_falconry", 43, 99,
                "Piscatoris falconry", "Piscatoris falconry area: rent a gyr falcon from Matthias, catch spotted kebbits (dark kebbits from 57), retrieve the falcon, drop the loot, and repeat.",
                TrainingIntensity.EFFICIENT, MethodCostTier.VERY_LOW, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 4,
                "500 coins for falcon rental");
        add(Skill.HUNTER, "hunter_deadfall_kebbits", 23, 56,
                "Deadfall kebbits", "Use the live two-deadfall limit and log-preservation behavior for Wild, Barb-tailed, Prickly, or Sabre-toothed kebbits appropriate to the current level.",
                TrainingIntensity.BALANCED, MethodCostTier.VERY_LOW, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 4,
                "Verified deadfall habitat", "Knife, axe, and a small log buffer");
        add(Skill.HUNTER, "hunter_deadfall_pyre_foxes", 57, 99,
                "Pyre fox deadfalls", "Hunt Pyre foxes with the live faster two-deadfall loop when their rewards, Rumour task, or active Hunter progress justify the travel.",
                TrainingIntensity.EFFICIENT, MethodCostTier.VERY_LOW, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 4,
                "Pyre fox habitat access", "Knife, axe, and a small log buffer");
        add(Skill.HUNTER, "hunter_maniacal_monkeys", 60, 99,
                "Maniacal monkeys", "Trap maniacal monkeys for low-attention Hunter when Kruk's Dungeon access and banana supply are ready.",
                TrainingIntensity.AFK, MethodCostTier.LOW, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.AFK, 25, 7,
                "Monkey Madness II Kruk's Dungeon access", "Banana supply");
        add(Skill.HUNTER, "hunter_sunlight_antelopes", 72, 90,
                "Sunlight antelopes", "Hunt sunlight antelopes when their antlers and meat are useful alongside active Hunter XP.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Sunlight antelope area access", "Pitfall tools and logs");
        add(Skill.HUNTER, "hunter_black_chins", 73, 99,
                "Black chinchompas", "Hunt black chinchompas only when the player explicitly accepts Wilderness risk and has an escape plan.",
                TrainingIntensity.EFFICIENT, MethodCostTier.PROFITABLE, RiskLevel.HIGH,
                false, true, false, false, true, AttentionLevel.ACTIVE, 15, 6,
                "Wilderness risk accepted", "Box traps and escape plan");
        add(Skill.HUNTER, "hunter_moonlight_antelopes", 91, 99,
                "Moonlight antelopes", "Hunt moonlight antelopes for high-level active Hunter and their useful antler/meat rewards.",
                TrainingIntensity.EFFICIENT, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Moonlight antelope area access", "Pitfall tools and logs");
    }

    private void production()
    {
        add(Skill.COOKING, "cooking_f2p_fish", 1, 99,
                "Cook fish at Al Kharid", "Al Kharid bank and range: withdraw one inventory of the selected raw fish, cook it on the range immediately north of the bank, bank, and repeat.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.LOW, 20, 2,
                "Raw fish supply");
        add(Skill.COOKING, "cooking_wines", 35, 99,
                "Jugs of wine", "Make jugs of wine for fast bankstanding Cooking when the grape cost/supply is acceptable.",
                TrainingIntensity.EFFICIENT, MethodCostTier.MODERATE, RiskLevel.NONE,
                true, false, false, true, false, AttentionLevel.ACTIVE, 20, 2,
                "Grape and jug-of-water supply");
        add(Skill.COOKING, "cooking_hosidius", 1, 99,
                "Hosidius kitchen", "Cook banked food in the Hosidius kitchen when its lower burn rate matters.",
                TrainingIntensity.RELAXED, MethodCostTier.FREE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 20, 4,
                "Hosidius kitchen access");
        add(Skill.COOKING, "cooking_karambwan_1t", 30, 99,
                "One-tick karambwans", "Use one-tick karambwan cooking for very high active Cooking experience.",
                TrainingIntensity.SWEATY, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, false, true, false, AttentionLevel.ACTIVE, 20, 4,
                "Raw karambwan supply and cooking access");
        add(Skill.COOKING, "cooking_gnome_restaurant", 29, 99,
                "Gnome Restaurant deliveries", "Prepare and deliver gnome food when varied Cooking, delivery rewards, and collection goals matter more than raw XP.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 6,
                "Gnome Restaurant access", "Gnome ingredient and transport setup");

        add(Skill.SMITHING, "smithing_f2p_platebodies", 48, 99,
                "F2P platebodies", "Smith the best practical platebody tier for solid F2P Smithing experience and alchable products.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.MODERATE, 20, 3,
                "Appropriate bar supply");
        add(Skill.SMITHING, "smithing_cannonballs", 35, 99,
                "Cannonballs", "Smith cannonballs when a low-attention session and future Slayer ammunition are valuable.",
                TrainingIntensity.AFK, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.AFK, 20, 4,
                "Dwarf Cannon", "Steel bar supply");
        add(Skill.SMITHING, "smithing_blast_furnace_gold", 40, 99,
                "Blast Furnace gold", "Smelt gold ore at Blast Furnace with the best verified gauntlet setup for fast Smithing experience.",
                TrainingIntensity.SWEATY, MethodCostTier.HIGH, RiskLevel.NONE,
                false, false, false, true, false, AttentionLevel.ACTIVE, 20, 7,
                "Blast Furnace access", "Gold ore supply");
        add(Skill.SMITHING, "smithing_giants_foundry", 15, 99,
                "Giants' Foundry", "Forge swords at Giants' Foundry for resource-efficient Smithing, profit, and outfit rewards.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 25, 6,
                "Giants' Foundry access", "Metal supply");
        add(Skill.SMITHING, "smithing_dart_tips", 4, 99,
                "Dart tips", "Smith dart tips for a slower, low-attention route when the bars have better strategic value as ammunition.",
                TrainingIntensity.AFK, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.AFK, 20, 3,
                "Dart Smithing unlock and bar supply");
        add(Skill.SMITHING, "smithing_blast_furnace_bars", 15, 99,
                "Blast Furnace bars", "Smelt the most strategically useful unlocked bar at Blast Furnace when coal savings, output value, or iron supply justify the setup.",
                TrainingIntensity.EFFICIENT, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 7,
                "Blast Furnace access and operating coins", "Ore, coal and transport setup");

        add(Skill.CRAFTING, "crafting_f2p_jewellery", 5, 99,
                "F2P jewellery", "Craft gold or gem jewellery appropriate to the available F2P materials and Magic plans.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.MODERATE, 20, 3,
                "Gold/gem supply and mould");
        add(Skill.CRAFTING, "crafting_glass", 1, 99,
                "Molten glass", "Make and blow molten glass for a self-source-friendly Crafting route.",
                TrainingIntensity.RELAXED, MethodCostTier.LOW, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 20, 5,
                "Glassmaking supplies");
        add(Skill.CRAFTING, "crafting_gems", 20, 99,
                "Cut gems", "Cut the best sensible banked or purchased gems for straightforward Crafting experience.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 2,
                "Uncut gem supply");
        add(Skill.CRAFTING, "crafting_battlestaves", 54, 99,
                "Battlestaves", "Charge or combine battlestaves when the orb/staff supply and alch economics are favorable.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 5,
                "Battlestaff and orb supply");
        add(Skill.CRAFTING, "crafting_dhide", 63, 99,
                "Dragonhide bodies", "Craft dragonhide bodies when hides are available and fast bankstanding experience is worth the cost.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 3,
                "Dragonhide and thread supply");
        add(Skill.CRAFTING, "crafting_charter_glass", 1, 99,
                "Charter-ship glass", "Buy or gather seaweed and sand near a charter route, make molten glass, and blow the best useful item when a bank-light loop suits the account.",
                TrainingIntensity.BALANCED, MethodCostTier.LOW, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 6,
                "Reachable charter ship stock", "Glassblowing pipe and glassmaking runes or furnace");

        add(Skill.FLETCHING, "fletching_arrow_shafts", 1, 20,
                "Arrow shafts", "Fletch logs into arrow shafts for cheap early Fletching and future ammunition supplies.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 10, 1,
                "Knife and log supply");
        add(Skill.FLETCHING, "fletching_bows", 5, 99,
                "Longbows and shortbows", "Fletch the best sensible bow tier for a relaxed, often inexpensive or profitable route.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 20, 2,
                "Knife and log supply");
        add(Skill.FLETCHING, "fletching_darts", 10, 99,
                "Darts", "Fletch dart tips and feathers during movement or downtime for fast zero-time-friendly experience.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.NONE,
                false, false, true, true, false, AttentionLevel.LOW, 10, 2,
                "Dart tip and feather supply");
        add(Skill.FLETCHING, "fletching_broad_arrows", 52, 99,
                "Broad arrows", "Make broad arrows for fast Fletching when Slayer unlocks and material supply are ready.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 4,
                "Broader Fletching Slayer unlock", "Broad arrow supply");
        add(Skill.FLETCHING, "fletching_bolts", 9, 99,
                "Bolts", "Fletch bolts when the ammunition is useful for the account or profitable to make.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 15, 2,
                "Bolt component supply");

        add(Skill.FIREMAKING, "firemaking_f2p_logs", 1, 99,
                "Burn logs", "Burn the best sensible F2P logs in a clear line for conventional Firemaking training.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.ACTIVE, 15, 2,
                "Tinderbox and log supply");
        add(Skill.FIREMAKING, "firemaking_wintertodt", 50, 99,
                "Wintertodt", "Subdue Wintertodt for Firemaking experience, collection-log progress, and useful supplies.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 5,
                "Wintertodt access and safe food/warm clothing");
        add(Skill.FIREMAKING, "firemaking_campfires", 1, 99,
                "Campfires", "Add logs to campfires when you prefer slower, lower-attention Firemaking over line lighting.",
                TrainingIntensity.RELAXED, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 20, 2,
                "Log supply");
        add(Skill.FIREMAKING, "firemaking_shades", 5, 99,
                "Shade pyres", "Burn the best safe shade remains and pyre logs available when Prayer XP, keys, and Shade reward progression are useful.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 6,
                "Shades of Mort'ton access", "Shade remains, pyre logs and tinderbox");

        add(Skill.RUNECRAFT, "runecraft_f2p_body", 20, 99,
                "F2P body runes", "Craft body runes or the best unlocked F2P rune using bank/altar routes that fit the account.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                true, true, true, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Talisman/tiara and essence supply");
        add(Skill.RUNECRAFT, "runecraft_lava", 23, 99,
                "Lava runes", "Craft lava runes with binding supplies and fast teleports for high active Runecraft experience.",
                TrainingIntensity.SWEATY, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, false, true, false, AttentionLevel.ACTIVE, 20, 8,
                "Fire altar access", "Binding necklace and earth-rune setup");
        add(Skill.RUNECRAFT, "runecraft_gotr", 27, 99,
                "Guardians of the Rift", "Play Guardians of the Rift for balanced Runecraft experience, outfit progress, and runes.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 5,
                "Temple of the Eye access");
        add(Skill.RUNECRAFT, "runecraft_zmi", 27, 99,
                "Ourania Altar", "Use the Ourania/ZMI altar for a lower-friction conventional Runecraft route.",
                TrainingIntensity.RELAXED, MethodCostTier.LOW, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 6,
                "Ourania Altar route and essence supply");
        add(Skill.RUNECRAFT, "runecraft_blood", 77, 99,
                "Blood runes", "Craft blood runes for relaxed high-level experience and a valuable rune supply.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 30, 5,
                "Unlocked blood-rune route");
        add(Skill.RUNECRAFT, "runecraft_soul", 90, 99,
                "Soul runes", "Craft soul runes for relaxed high-level Runecraft experience when the route is unlocked.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 30, 5,
                "Unlocked soul-rune route");
        add(Skill.RUNECRAFT, "runecraft_abyss", 1, 99,
                "Abyss altar runs", "Use the Abyss for the most useful unlocked altar only when the short Wilderness crossing and required protection are accepted.",
                TrainingIntensity.EFFICIENT, MethodCostTier.PROFITABLE, RiskLevel.HIGH,
                false, true, false, false, true, AttentionLevel.ACTIVE, 20, 6,
                "Enter the Abyss", "Essence, pouches and altar access", "Wilderness risk accepted");

        add(Skill.HERBLORE, "herblore_low_potions", 3, 37,
                "Early potions", "Clean herbs and make the best useful low-level potion supported by the account's herb and secondary supply.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 15, 3,
                "Herb and secondary supply");
        add(Skill.HERBLORE, "herblore_prayer_potions", 38, 62,
                "Prayer potions", "Make prayer potions when ranarrs and snape grass are strategically available.",
                TrainingIntensity.BALANCED, MethodCostTier.HIGH, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 3,
                "Ranarr and snape grass supply");
        add(Skill.HERBLORE, "herblore_restores", 63, 80,
                "Super restores", "Make super restores when the account has sufficient snapdragon and red spider egg supply.",
                TrainingIntensity.BALANCED, MethodCostTier.HIGH, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 3,
                "Snapdragon and red spider egg supply");
        add(Skill.HERBLORE, "herblore_brews", 81, 99,
                "Saradomin brews", "Make brews when toadflax and crushed nests are available and the PvM supply value is high.",
                TrainingIntensity.EFFICIENT, MethodCostTier.VERY_HIGH, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 3,
                "Toadflax and crushed nest supply");
        add(Skill.HERBLORE, "herblore_mixology", 60, 99,
                "Mastering Mixology", "Use Mastering Mixology for reagent-efficient Herblore and its progression rewards.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 25, 6,
                "Mastering Mixology access and reagent supply");
    }

    private void utility()
    {
        add(Skill.AGILITY, "agility_rooftops", 1, 99,
                "Rooftop courses", "Run the best verified rooftop course for the current level, balancing XP and Marks of grace.",
                TrainingIntensity.BALANCED, MethodCostTier.FREE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 3,
                "Reachable rooftop course");
        add(Skill.AGILITY, "agility_canifis_marks", 40, 59,
                "Canifis rooftop for Graceful", "Prioritize Canifis rooftops while Marks of grace and Graceful remain a protected progression objective.",
                TrainingIntensity.BALANCED, MethodCostTier.FREE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 3,
                "Canifis access");
        add(Skill.AGILITY, "agility_seers", 60, 79,
                "Seers' Village rooftop", "Run Seers' Village rooftop for strong conventional Agility and Marks of grace progression.",
                TrainingIntensity.EFFICIENT, MethodCostTier.FREE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 3,
                "Seers' Village course access");
        add(Skill.AGILITY, "agility_sep", 52, 99,
                "Hallowed Sepulchre", "Run the deepest unlocked Hallowed Sepulchre floors for high active Agility XP and valuable rewards.",
                TrainingIntensity.SWEATY, MethodCostTier.PROFITABLE, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 6,
                "Hallowed Sepulchre access");
        add(Skill.AGILITY, "agility_prif", 75, 99,
                "Prifddinas Agility Course", "Use the Prifddinas course for a relaxed high-level rooftop-style alternative when unlocked.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 4,
                "Prifddinas access");
        add(Skill.AGILITY, "agility_wildy_expanded", 52, 99,
                "Wilderness Agility Course", "Use the Wilderness course only when the player explicitly accepts Wilderness risk.",
                TrainingIntensity.EFFICIENT, MethodCostTier.PROFITABLE, RiskLevel.HIGH,
                false, true, false, false, true, AttentionLevel.ACTIVE, 20, 7,
                "Wilderness risk accepted");
        add(Skill.AGILITY, "agility_colossal_wyrm", 50, 99,
                "Colossal Wyrm courses", "Run the live longer basic or advanced course when its lower-input rhythm, bone shards, and termite rewards fit the session; do not use the pre-update per-lap XP value.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 20, 4,
                "Colossal Wyrm course access");
        add(Skill.AGILITY, "agility_brimhaven_arena", 1, 99,
                "Brimhaven Agility Arena", "Tag the active arena pillars and spend tickets when the varied obstacle loop and reward goals fit the session.",
                TrainingIntensity.BALANCED, MethodCostTier.LOW, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Brimhaven arena access and entrance coins");

        add(Skill.THIEVING, "thieving_fruit_stalls", 25, 54,
                "Hosidius fruit stalls", "Steal from both fruit stalls in the easternmost house near the Hosidius beach, drop the fruit while moving between them, and repeat.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 15, 3);
        add(Skill.THIEVING, "thieving_lumbridge_people", 1, 24,
                "Lumbridge pickpockets", "Pickpocket men and women around Lumbridge Castle, use the nearby bank for food, and repeat.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.MODERATE, 15, 1);
        add(Skill.THIEVING, "thieving_blackjack", 45, 99,
                "Blackjacking", "Blackjack appropriate NPCs for very high, click-intensive Thieving experience.",
                TrainingIntensity.SWEATY, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, false, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Blackjacking quest/access requirements");
        add(Skill.THIEVING, "thieving_ardy_knights", 55, 99,
                "Ardougne knights", "Ardougne Marketplace: pickpocket a knight already lured into a house or bank safespot, open coin pouches before the cap, heal, and repeat.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.LOW, 20, 4,
                "Food or healing supply");
        add(Skill.THIEVING, "thieving_pyramid", 21, 99,
                "Pyramid Plunder", "Run Pyramid Plunder for active Thieving XP and sceptre collection-log progression.",
                TrainingIntensity.EFFICIENT, MethodCostTier.PROFITABLE, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 6,
                "Sophanem/Pyramid Plunder access");
        add(Skill.THIEVING, "thieving_varlamore", 50, 99,
                "Varlamore wealthy citizens", "Use Varlamore pickpocketing and house robbery loops for lower-intensity Thieving.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.LOW, 20, 5,
                "Varlamore Thieving access");
        add(Skill.THIEVING, "thieving_artefacts", 49, 99,
                "Stealing artefacts", "Steal and deliver artefacts in Port Piscarilius for active Thieving with useful multiskill movement windows.",
                TrainingIntensity.EFFICIENT, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 6,
                "Port Piscarilius artefact access", "Guard route and stamina plan");
        add(Skill.THIEVING, "thieving_stone_chests", 64, 99,
                "Stone chests", "Loot stone chests when medium clues, gems, and lower-intensity Thieving rewards are strategically useful.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.LOW, 20, 5,
                "Lizardman Temple chest access", "Food or healing plan");
        add(Skill.THIEVING, "thieving_vyres", 82, 99,
                "Vyres", "Pickpocket vyres when blood shards, profit, and a sustainable bank/altar route outweigh faster XP methods.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.LOW, 20, 6,
                "Sins of the Father and Darkmeyer access", "Vyre outfit and healing setup");
        add(Skill.THIEVING, "thieving_elves", 85, 99,
                "Prifddinas elves", "Pickpocket an appropriate elf clan when crystal shards, teleport seeds, and profit are the priority.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.LOW, 20, 6,
                "Song of the Elves and Prifddinas access", "Healing and dodgy-necklace setup");
        add(Skill.THIEVING, "thieving_rogues_chest", 84, 99,
                "Rogues' Castle chests", "Loot Rogues' Castle chests only when Wilderness risk is explicitly accepted and the account has an escape/death-loss plan.",
                TrainingIntensity.EFFICIENT, MethodCostTier.PROFITABLE, RiskLevel.HIGH,
                false, true, false, false, true, AttentionLevel.ACTIVE, 15, 7,
                "Wilderness risk accepted", "Deep-Wilderness escape and loss plan");

        add(Skill.SLAYER, "slayer_safe_assignments", 1, 99,
                "Conservative Slayer", "Use the best safe unlocked Slayer master and favor low-risk assignments with a reliable escape/supply plan.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.MODERATE, 30, 6);
        add(Skill.SLAYER, "slayer_highest_master", 1, 99,
                "Highest practical Slayer master", "Use the highest practical unlocked master, accounting for block list, points, gear, and task suitability.",
                TrainingIntensity.EFFICIENT, MethodCostTier.PROFITABLE, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.MODERATE, 30, 6,
                "Unlocked Slayer master and task state");
        add(Skill.SLAYER, "slayer_burst_tasks", 65, 99,
                "Burst/Barrage Slayer", "Prioritize unlocked multi-target tasks that can be safely burst or barraged when rune supply supports it.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.ACTIVE, 30, 8,
                "Suitable burst task", "Ancient Magicks and rune supply");
        add(Skill.SLAYER, "slayer_cannon_tasks", 1, 99,
                "Cannon Slayer", "Use a cannon on eligible tasks when cannonball cost or self-sourced supply is justified by the time saved.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.MODERATE, 30, 8,
                "Dwarf Cannon", "Cannon-eligible task", "Cannonball supply");
        add(Skill.SLAYER, "slayer_bossing", 75, 99,
                "Slayer bossing", "Use suitable boss variants only when gear, supplies, experience, and risk settings support them.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.HIGH,
                false, true, true, false, false, AttentionLevel.ACTIVE, 30, 10,
                "Boss task and PvM readiness");
        add(Skill.SLAYER, "slayer_point_boosting", 1, 99,
                "Slayer point boosting", "Use short low-tier tasks before each bonus task only when the points unlock a worthwhile block, extension, or item sooner than normal Slayer.",
                TrainingIntensity.EFFICIENT, MethodCostTier.LOW, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.MODERATE, 30, 6,
                "Two practical Slayer masters and live task streak", "Point-spend goal");
        add(Skill.SLAYER, "slayer_wilderness", 1, 99,
                "Wilderness Slayer", "Use Wilderness Slayer only when explicitly enabled, with task-specific risk, death-loss, and escape planning.",
                TrainingIntensity.EFFICIENT, MethodCostTier.PROFITABLE, RiskLevel.HIGH,
                false, true, false, false, true, AttentionLevel.ACTIVE, 30, 8,
                "Wilderness risk accepted", "Krystilia access and disposable setup");

        add(Skill.FARMING, "farming_allotments_expanded", 1, 99,
                "Allotment runs", "Plant the best useful available allotments when seeds, tools, compost, and reachable patches are confirmed.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 10, 5,
                "Reachable allotment patches and supplies");
        add(Skill.FARMING, "farming_falador_potatoes", 1, 14,
                "Falador potato allotments", "South Falador Farm: rake one allotment, plant three potato seeds, leave it to grow, return with a spade, harvest, and replant.",
                TrainingIntensity.BALANCED, MethodCostTier.VERY_LOW, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 10, 2,
                "Falador patch, potato seeds, and farming tools");
        add(Skill.FARMING, "farming_falador_watermelons", 47, 99,
                "Falador watermelon allotments", "South Falador Farm: rake one allotment, plant three watermelon seeds, leave it to grow, return with a spade, harvest, and replant.",
                TrainingIntensity.BALANCED, MethodCostTier.LOW, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 10, 2,
                "Falador patch, watermelon seeds, and farming tools");
        add(Skill.FARMING, "farming_herbs_expanded", 9, 99,
                "Herb runs", "Run verified herb patches with the best strategically useful seed supply available.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 10, 5,
                "Herb seeds, tools, compost, teleports and reachable patches");
        add(Skill.FARMING, "farming_trees", 15, 99,
                "Tree runs", "Plant and check the best sensible tree tier supported by the account's seed and payment supply.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 10, 6,
                "Tree seeds, payments and reachable patches");
        add(Skill.FARMING, "farming_fruit_trees", 27, 99,
                "Fruit tree runs", "Add fruit trees to Farming runs when seeds and transport unlocks make the route worthwhile.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 12, 7,
                "Fruit tree seeds and reachable patches");
        add(Skill.FARMING, "farming_tithe", 34, 99,
                "Tithe Farm", "Use Tithe Farm for active Farming experience and its useful unlock rewards when a continuous session fits.",
                TrainingIntensity.SWEATY, MethodCostTier.FREE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.ACTIVE, 30, 4,
                "Tithe Farm access");
        add(Skill.FARMING, "farming_contracts", 45, 99,
                "Farming contracts", "Complete Farming Guild contracts opportunistically to build a sustainable seed supply.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 5, 3,
                "Farming Guild contract access");
        add(Skill.FARMING, "farming_seaweed", 23, 99,
                "Giant seaweed runs", "Plant and harvest giant seaweed when Crafting glass supply and a short recurring run fit the account.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 8, 6,
                "Fossil Island underwater seaweed patches", "Seaweed spores, compost and diving setup");
        add(Skill.FARMING, "farming_hardwood", 35, 99,
                "Hardwood tree runs", "Maintain teak, mahogany, and other unlocked hardwood patches for infrequent high-value Farming cycles.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.LOW, 8, 7,
                "Reachable hardwood patches", "Saplings, compost and payment plan");
        add(Skill.FARMING, "farming_hespori", 65, 99,
                "Hespori", "Plant and defeat Hespori when ready for Farming XP, anima seeds, and bottomless-bucket progression.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.MODERATE, 10, 6,
                "Farming Guild Hespori cave access", "Hespori seed and combat readiness");

        add(Skill.CONSTRUCTION, "construction_oak_larders", 33, 73,
                "Oak larders", "At the oak larder hotspot in a verified POH Kitchen, enter building mode, build an oak larder with eight oak planks, remove it, and repeat.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Verified POH Kitchen, oak planks, hammer, and saw");
        add(Skill.CONSTRUCTION, "construction_crude_chairs", 1, 32,
                "Crude wooden chairs", "In a verified POH Parlour, enter building mode, build a crude wooden chair with two planks and two nails, remove it, and repeat.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.ACTIVE, 15, 3,
                "Verified POH Parlour, planks, nails, hammer, and saw");
        add(Skill.CONSTRUCTION, "construction_oak_doors", 74, 99,
                "Oak dungeon doors", "Build and remove oak dungeon doors for fast high-level oak-plank Construction.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Dungeon room and oak plank supply");
        add(Skill.CONSTRUCTION, "construction_mahogany_tables", 52, 99,
                "Mahogany tables", "Build mahogany tables when maximum conventional Construction speed justifies the plank cost.",
                TrainingIntensity.SWEATY, MethodCostTier.VERY_HIGH, RiskLevel.NONE,
                false, false, false, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Dining room and mahogany plank supply");
        add(Skill.CONSTRUCTION, "construction_mahogany_homes", 1, 99,
                "Mahogany Homes", "Complete Mahogany Homes contracts for lower-cost Construction and outfit/tool rewards.",
                TrainingIntensity.BALANCED, MethodCostTier.MODERATE, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 6,
                "Mahogany Homes contract and plank supply");
        add(Skill.CONSTRUCTION, "construction_mythical_capes", 47, 99,
                "Mounted mythical capes", "Build and remove mounted mythical capes when teak-plank efficiency matters and Dragon Slayer II is complete.",
                TrainingIntensity.EFFICIENT, MethodCostTier.HIGH, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 6,
                "Dragon Slayer II", "Quest hall, teak planks and mythical cape");
        add(Skill.CONSTRUCTION, "construction_teak_benches", 66, 99,
                "Teak garden benches", "Build and remove teak garden benches for fast active Construction when the plank cost and POH setup are justified.",
                TrainingIntensity.SWEATY, MethodCostTier.HIGH, RiskLevel.NONE,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 5,
                "Superior garden and teak plank supply");
    }

    private void sailing()
    {
        add(Skill.SAILING, "sailing_charting", 1, 99,
                "Sea charting", "Complete reachable one-time sea charting objectives while moving through Sailing progression.",
                TrainingIntensity.EFFICIENT, MethodCostTier.FREE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.MODERATE, 15, 5,
                "Pandemonium completed", "Required charting tools and reachable sea region");
        add(Skill.SAILING, "sailing_courier", 1, 99,
                "Courier port tasks", "Stack compatible courier tasks between unlocked ports and combine them with other Sailing travel when practical.",
                TrainingIntensity.BALANCED, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.MODERATE, 20, 5,
                "Pandemonium completed", "Boat and unlocked notice boards");
        add(Skill.SAILING, "sailing_salvage_small", 15, 99,
                "Shipwreck salvaging", "Salvage the best safe unlocked shipwreck tier for relaxed Sailing experience and loot.",
                TrainingIntensity.AFK, MethodCostTier.PROFITABLE, RiskLevel.LOW,
                false, true, true, true, false, AttentionLevel.AFK, 20, 6,
                "Suitable salvaging hook", "Boat safe for the selected waters");
        add(Skill.SAILING, "sailing_barracuda_tantrum", 30, 54,
                "Barracuda Trials: Tempor Tantrum", "Run the first Barracuda Trial when the boat and route are verified ready.",
                TrainingIntensity.EFFICIENT, MethodCostTier.LOW, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 8,
                "Tempor Tantrum unlocked", "Trial-ready boat");
        add(Skill.SAILING, "sailing_barracuda_jubbly", 55, 71,
                "Barracuda Trials: Jubbly Jive", "Run Jubbly Jive for active mid-level Sailing when unlocked and the boat is ready.",
                TrainingIntensity.SWEATY, MethodCostTier.LOW, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 8,
                "Jubbly Jive unlocked", "Trial-ready boat");
        add(Skill.SAILING, "sailing_barracuda_gwenith", 72, 99,
                "Barracuda Trials: Gwenith Glide", "Run Gwenith Glide for high-level active Sailing when quest and boat requirements are verified.",
                TrainingIntensity.SWEATY, MethodCostTier.MODERATE, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.ACTIVE, 20, 10,
                "Gwenith Glide and Regicide access", "High-level trial-ready boat");
        add(Skill.SAILING, "sailing_deep_sea_trawling", 60, 99,
                "Deep Sea Trawling", "Use Deep Sea Trawling when hybrid Fishing/Sailing progress and valuable fish matter more than maximum Sailing XP.",
                TrainingIntensity.RELAXED, MethodCostTier.PROFITABLE, RiskLevel.MEDIUM,
                false, true, true, true, false, AttentionLevel.LOW, 30, 10,
                "Deep Sea Trawling access and safe boat setup");
    }

    private static String id(Skill skill, String suffix)
    {
        return skill.name().toLowerCase() + "_" + suffix;
    }

    private void add(
            Skill skill,
            String id,
            int minLevel,
            int maxLevel,
            String name,
            String instructions,
            TrainingIntensity intensity,
            MethodCostTier cost,
            RiskLevel risk,
            boolean f2p,
            boolean selfSourceFriendly,
            boolean uimFriendly,
            boolean hardcoreSafe,
            boolean wilderness,
            AttentionLevel attention,
            int minimumSessionMinutes,
            int setupMinutes,
            String... requirements)
    {
        double[] scores = scores(intensity);
        List<String> requirementList = Arrays.asList(requirements);
        RecommendationConfidence confidence = requirementList.isEmpty()
                ? RecommendationConfidence.VERIFIED
                : RecommendationConfidence.CHECK_NEEDED;
        TrainingMethod method = new TrainingMethod(
                id, skill, minLevel, maxLevel, name, instructions,
                scores[0], scores[1], scores[2], attention,
                minimumSessionMinutes, setupMinutes, requirementList,
                confidence, !f2p, wilderness, false);
        TrainingMethodMetadata metadata = new TrainingMethodMetadata(
                intensity, cost, risk, f2p, selfSourceFriendly,
                uimFriendly, hardcoreSafe, Collections.emptyList());
        methods.get(skill).add(new CuratedTrainingMethod(method, metadata));
    }

    private static double[] scores(TrainingIntensity intensity)
    {
        switch (intensity)
        {
            case SWEATY: return new double[]{22.0, 9.0, -4.0};
            case EFFICIENT: return new double[]{19.0, 15.0, 5.0};
            case RELAXED: return new double[]{8.0, 14.0, 19.0};
            case AFK: return new double[]{5.0, 11.0, 22.0};
            case BALANCED:
            default: return new double[]{14.0, 17.0, 12.0};
        }
    }
}
