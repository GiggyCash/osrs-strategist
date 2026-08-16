package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Starter data catalog for training methods. The engine is intentionally data
 * driven: future OSRS updates should usually add or adjust method data instead
 * of rewriting recommendation logic.
 */
@Singleton
public class TrainingMethodDatabase
{
    private final List<TrainingMethod> methods = new ArrayList<>();

    public TrainingMethodDatabase()
    {
        addCoreMethods();
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

    private void addCoreMethods()
    {
        add("attack_combat", Skill.ATTACK, 1, 99,
                "Train Attack during normal combat",
                "Use the best melee weapon you can equip and favor monsters or Slayer tasks you can kill comfortably.",
                8, 10, 8, AttentionLevel.MODERATE, 10, 2,
                none(), RecommendationConfidence.VERIFIED);

        add("strength_combat", Skill.STRENGTH, 1, 99,
                "Train Strength during normal combat",
                "Use a Strength-training attack style with your best practical melee setup on safe, sustainable targets.",
                9, 10, 8, AttentionLevel.MODERATE, 10, 2,
                none(), RecommendationConfidence.VERIFIED);

        add("defence_combat", Skill.DEFENCE, 1, 99,
                "Train Defence during normal combat",
                "Use a Defence-training attack style while doing combat that also advances Slayer, drops, or account unlocks when possible.",
                8, 10, 8, AttentionLevel.MODERATE, 10, 2,
                none(), RecommendationConfidence.VERIFIED);

        add("ranged_combat", Skill.RANGED, 1, 99,
                "Train Ranged during useful combat",
                "Use the best practical ranged weapon and ammunition you can sustain, preferably while progressing Slayer or useful drops.",
                9, 10, 8, AttentionLevel.MODERATE, 10, 3,
                none(), RecommendationConfidence.VERIFIED);

        add("magic_utility", Skill.MAGIC, 1, 99,
                "Combine utility spells with combat Magic",
                "Use useful teleports, enchants, alchemy, and combat spells you already have resources for instead of spending runes only for raw XP.",
                9, 11, 8, AttentionLevel.MODERATE, 10, 3,
                none(), RecommendationConfidence.VERIFIED);

        add("prayer_bones", Skill.PRAYER, 1, 99,
                "Use the best bones your account can sustainably obtain",
                "Prefer banked or naturally obtained bones first. Use the safest altar or prayer-training option your account has actually unlocked.",
                10, 11, 7, AttentionLevel.MODERATE, 10, 5,
                Arrays.asList("Confirm the altar/training location is unlocked"),
                RecommendationConfidence.CHECK_NEEDED);

        add("runecraft_best_rune", Skill.RUNECRAFT, 1, 99,
                "Craft the most useful rune currently available",
                "Favor runes your account will actually consume for Magic, teleports, or PvM rather than training in isolation.",
                8, 10, 7, AttentionLevel.MODERATE, 15, 5,
                Arrays.asList("Rune essence or suitable Runecraft supplies"),
                RecommendationConfidence.CHECK_NEEDED);

        add("runecraft_gotr", Skill.RUNECRAFT, 27, 99,
                "Guardians of the Rift",
                "Use Guardians of the Rift when unlocked for a more varied Runecraft session with useful rewards and outfit progression.",
                11, 12, 10, AttentionLevel.ACTIVE, 30, 5,
                Arrays.asList("Guardians of the Rift access", "Required quest/access unlocks"),
                RecommendationConfidence.CHECK_NEEDED);

        add("construction_standard", Skill.CONSTRUCTION, 1, 99,
                "Use banked planks on the best practical furniture",
                "Spend planks you already own first and choose a build/remove cycle that fits your level and budget.",
                12, 10, 5, AttentionLevel.ACTIVE, 15, 5,
                Arrays.asList("Player-owned house access", "Planks and required materials"),
                RecommendationConfidence.CHECK_NEEDED);

        add("construction_homes", Skill.CONSTRUCTION, 20, 99,
                "Mahogany Homes",
                "Use Mahogany Homes when available for lower material burn and more varied Construction training.",
                8, 12, 11, AttentionLevel.MODERATE, 30, 5,
                Arrays.asList("Mahogany Homes access", "Required planks and teleports"),
                RecommendationConfidence.CHECK_NEEDED);

        add("agility_rooftop", Skill.AGILITY, 1, 99,
                "Best unlocked rooftop or Agility course",
                "Use the highest sensible course you have unlocked, prioritizing Marks of Grace when Graceful or stamina resources still matter.",
                11, 12, 8, AttentionLevel.ACTIVE, 20, 2,
                none(), RecommendationConfidence.CHECK_NEEDED);

        add("herblore_bank", Skill.HERBLORE, 1, 99,
                "Process banked herbs into useful potions",
                "Use herbs and secondaries you already own, prioritizing potions your account will actually use later.",
                11, 13, 9, AttentionLevel.MODERATE, 10, 3,
                Arrays.asList("Herblore must be unlocked", "Usable herbs and secondaries"),
                RecommendationConfidence.CHECK_NEEDED);

        add("thieving_best_target", Skill.THIEVING, 1, 99,
                "Best unlocked low-risk Thieving target",
                "Use a stall, chest, or pickpocket target appropriate to your level and food supply, favoring useful loot when efficiency is close.",
                11, 11, 8, AttentionLevel.ACTIVE, 15, 2,
                none(), RecommendationConfidence.CHECK_NEEDED);

        add("crafting_banked", Skill.CRAFTING, 1, 99,
                "Process banked Crafting supplies",
                "Use gems, hides, glass materials, jewelry supplies, or other banked resources before gathering or buying more.",
                10, 13, 9, AttentionLevel.MODERATE, 10, 3,
                Arrays.asList("Confirm which Crafting supplies are currently available"),
                RecommendationConfidence.CHECK_NEEDED);

        add("fletching_logs", Skill.FLETCHING, 1, 99,
                "Fletch banked logs into useful products",
                "Use logs you already have and favor products that support future Ranged, alching, or money-making plans.",
                9, 12, 12, AttentionLevel.LOW, 10, 1,
                Arrays.asList("Logs or other Fletching supplies"),
                RecommendationConfidence.CHECK_NEEDED);

        add("slayer_tasks", Skill.SLAYER, 1, 99,
                "Complete Slayer assignments",
                "Use the strongest Slayer master you can access without making tasks unnecessarily slow, and train combat styles that also need levels.",
                12, 14, 9, AttentionLevel.MODERATE, 30, 5,
                Arrays.asList("A suitable Slayer master and task"),
                RecommendationConfidence.CHECK_NEEDED);

        add("hunter_traps", Skill.HUNTER, 1, 99,
                "Best unlocked Hunter creature or trap",
                "Use the strongest practical Hunter method unlocked for your level, favoring useful supplies or collection progress when rates are similar.",
                10, 11, 8, AttentionLevel.MODERATE, 15, 5,
                none(), RecommendationConfidence.CHECK_NEEDED);

        add("hunter_birdhouses", Skill.HUNTER, 5, 99,
                "Birdhouse runs",
                "Use birdhouse runs as recurring passive Hunter progress when Fossil Island access and the required supplies are confirmed.",
                13, 15, 14, AttentionLevel.LOW, 5, 5,
                Arrays.asList("Bone Voyage/Fossil Island access", "Birdhouse supplies and transport"),
                RecommendationConfidence.CHECK_NEEDED);

        add("mining_ore", Skill.MINING, 1, 99,
                "Mine the best useful ore you can access",
                "Choose ore that supports Smithing, Crafting, quests, or money needs when the XP tradeoff is reasonable.",
                10, 12, 9, AttentionLevel.MODERATE, 15, 3,
                none(), RecommendationConfidence.VERIFIED);

        add("mining_mlm", Skill.MINING, 30, 99,
                "Motherlode Mine",
                "Use Motherlode Mine when available for lower-attention Mining, ores, and Prospector progression.",
                8, 12, 14, AttentionLevel.LOW, 30, 3,
                Arrays.asList("Motherlode Mine access"),
                RecommendationConfidence.CHECK_NEEDED);

        add("smithing_banked", Skill.SMITHING, 1, 99,
                "Smith banked ores and bars",
                "Use resources already owned first, favoring products that help quests, equipment, ammunition, or later processing.",
                9, 12, 9, AttentionLevel.MODERATE, 10, 3,
                Arrays.asList("Usable ores/bars and furnace or anvil access"),
                RecommendationConfidence.CHECK_NEEDED);

        add("smithing_foundry", Skill.SMITHING, 15, 99,
                "Giants' Foundry",
                "Use Giants' Foundry when unlocked for material-efficient Smithing and Smiths' Uniform progression.",
                11, 13, 9, AttentionLevel.ACTIVE, 30, 5,
                Arrays.asList("Giants' Foundry access", "Suitable metal supplies"),
                RecommendationConfidence.CHECK_NEEDED);

        add("fishing_best", Skill.FISHING, 1, 99,
                "Fish the best useful catch currently available",
                "Favor fish your account can cook or use for PvM when XP differences are modest.",
                10, 12, 13, AttentionLevel.LOW, 15, 3,
                none(), RecommendationConfidence.VERIFIED);

        add("fishing_tempoross", Skill.FISHING, 35, 99,
                "Tempoross",
                "Use Tempoross for active Fishing with useful rewards and collection progression when you want variety.",
                10, 12, 8, AttentionLevel.ACTIVE, 30, 5,
                Arrays.asList("Tempoross access"),
                RecommendationConfidence.CHECK_NEEDED);

        add("cooking_banked", Skill.COOKING, 1, 99,
                "Cook banked raw food",
                "Turn food you already own into useful combat supplies before buying or gathering dedicated Cooking materials.",
                10, 13, 12, AttentionLevel.LOW, 10, 2,
                Arrays.asList("Raw cookable food"),
                RecommendationConfidence.CHECK_NEEDED);

        add("firemaking_logs", Skill.FIREMAKING, 1, 99,
                "Burn banked logs",
                "Use spare logs when you only need a short Firemaking push and do not want to set up a longer activity.",
                9, 9, 8, AttentionLevel.MODERATE, 10, 1,
                Arrays.asList("Logs and a tinderbox"),
                RecommendationConfidence.VERIFIED);

        add("firemaking_wintertodt", Skill.FIREMAKING, 50, 99,
                "Wintertodt",
                "Use Wintertodt for Firemaking XP plus supply and collection rewards when your food and access are ready.",
                12, 13, 9, AttentionLevel.ACTIVE, 30, 5,
                Arrays.asList("50 Firemaking", "Wintertodt access", "Suitable food/warm clothing"),
                RecommendationConfidence.CHECK_NEEDED);

        add("woodcutting_best", Skill.WOODCUTTING, 1, 99,
                "Cut the best useful tree currently available",
                "Favor logs needed for Fletching, Firemaking, Construction, birdhouses, or quests when rates are close.",
                10, 12, 14, AttentionLevel.LOW, 15, 2,
                none(), RecommendationConfidence.VERIFIED);

        add("woodcutting_forestry", Skill.WOODCUTTING, 1, 99,
                "Forestry-enabled Woodcutting",
                "Train around Forestry activity when available for variety and additional Woodcutting rewards.",
                9, 12, 10, AttentionLevel.MODERATE, 20, 3,
                Arrays.asList("A suitable Forestry area and axe"),
                RecommendationConfidence.CHECK_NEEDED);

        add("farming_early", Skill.FARMING, 1, 99,
                "Use the best unlocked Farming patches and seeds",
                "Start with whatever allotment, flower, tree, or other patches are actually unlocked, using banked seeds and compost first.",
                11, 14, 11, AttentionLevel.LOW, 10, 5,
                Arrays.asList("Confirm reachable Farming patches", "Seeds and farming tools"),
                RecommendationConfidence.CHECK_NEEDED);

        add("farming_herbs", Skill.FARMING, 9, 99,
                "Herb runs",
                "Use reachable herb patches as recurring Farming progress when seeds, tools, compost, and transport are ready.",
                13, 15, 13, AttentionLevel.LOW, 10, 5,
                Arrays.asList("9 Farming", "Herb seeds", "Tools/Tool Leprechaun state", "Reachable herb patches"),
                RecommendationConfidence.CHECK_NEEDED);

        add("sailing_unlocked", Skill.SAILING, 1, 99,
                "Best unlocked Sailing activity for your current port progression",
                "Choose a Sailing activity that advances both Sailing XP and useful voyage, port, contract, or resource progression. Strategist will replace this generic entry with verified activity data as Sailing coverage expands.",
                10, 12, 9, AttentionLevel.MODERATE, 20, 5,
                Arrays.asList("Confirm the Sailing activity and port are unlocked"),
                RecommendationConfidence.CHECK_NEEDED);
    }

    private void add(
            String id,
            Skill skill,
            int minLevel,
            int maxLevel,
            String name,
            String instructions,
            double efficientScore,
            double balancedScore,
            double relaxedScore,
            AttentionLevel attentionLevel,
            int minimumSessionMinutes,
            int setupMinutes,
            List<String> requirements,
            RecommendationConfidence confidence)
    {
        methods.add(
                new TrainingMethod(
                        id,
                        skill,
                        minLevel,
                        maxLevel,
                        name,
                        instructions,
                        efficientScore,
                        balancedScore,
                        relaxedScore,
                        attentionLevel,
                        minimumSessionMinutes,
                        setupMinutes,
                        requirements,
                        confidence
                )
        );
    }

    private static List<String> none()
    {
        return Collections.emptyList();
    }
}
