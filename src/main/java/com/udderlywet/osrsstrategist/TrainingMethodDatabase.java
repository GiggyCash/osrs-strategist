package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Data-driven training method catalog. */
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
            if (method.getSkill() == skill) result.add(method);
        }
        return Collections.unmodifiableList(result);
    }

    private void addCoreMethods()
    {
        add("attack_combat", Skill.ATTACK, 1, 99,
                "Train Attack during normal combat",
                "Use the best melee weapon you can equip and favor monsters or Slayer tasks you can kill comfortably.",
                8, 10, 8, AttentionLevel.MODERATE, 10, 2, none(), RecommendationConfidence.VERIFIED);
        add("strength_combat", Skill.STRENGTH, 1, 99,
                "Train Strength during normal combat",
                "Use a Strength-training attack style with your best practical melee setup on safe, sustainable targets.",
                9, 10, 8, AttentionLevel.MODERATE, 10, 2, none(), RecommendationConfidence.VERIFIED);
        add("defence_combat", Skill.DEFENCE, 1, 99,
                "Train Defence during normal combat",
                "Use a Defence-training attack style while doing combat that also advances Slayer, drops, or account unlocks when possible.",
                8, 10, 8, AttentionLevel.MODERATE, 10, 2, none(), RecommendationConfidence.VERIFIED);
        add("ranged_combat", Skill.RANGED, 1, 99,
                "Train Ranged during useful combat",
                "Use the best practical ranged weapon and ammunition you can sustain, preferably while progressing Slayer or useful drops.",
                9, 10, 8, AttentionLevel.MODERATE, 10, 3, none(), RecommendationConfidence.VERIFIED);
        addGeneric("magic_utility", Skill.MAGIC, 1, 99,
                "Combine utility spells with combat Magic",
                "Use useful teleports, enchants, alchemy, and combat spells you have confirmed resources for instead of spending runes only for raw XP.",
                9, 11, 8, AttentionLevel.MODERATE, 10, 3,
                none(), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("prayer_bones", Skill.PRAYER, 1, 99,
                "Use the best bones your account can sustainably obtain",
                "Prefer bones already confirmed available or naturally obtained first. Use the safest altar or prayer-training option your account has actually unlocked.",
                10, 11, 7, AttentionLevel.MODERATE, 10, 5,
                Arrays.asList("Confirm available bones", "Confirm the altar/training location is unlocked"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("runecraft_best_rune", Skill.RUNECRAFT, 1, 99,
                "Craft the most useful rune currently available",
                "Favor runes your account will actually consume for Magic, teleports, or PvM rather than training in isolation.",
                8, 10, 7, AttentionLevel.MODERATE, 15, 5,
                Arrays.asList("Rune essence or suitable Runecraft supplies"), RecommendationConfidence.CHECK_NEEDED);
        addProtected("runecraft_gotr", Skill.RUNECRAFT, 27, 99,
                "Guardians of the Rift",
                "Use Guardians of the Rift when unlocked for varied Runecraft training plus useful rewards and outfit progression.",
                11, 12, 10, AttentionLevel.ACTIVE, 30, 5,
                Arrays.asList("Guardians of the Rift access", "Required quest/access unlocks"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("construction_standard", Skill.CONSTRUCTION, 1, 99,
                "Use confirmed planks on the best practical furniture",
                "If plank supplies are confirmed available, choose a build/remove cycle that fits your level and budget.",
                12, 10, 5, AttentionLevel.ACTIVE, 15, 5,
                Arrays.asList("Player-owned house access", "Confirm planks and required materials"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("construction_homes", Skill.CONSTRUCTION, 20, 99,
                "Mahogany Homes",
                "Use Mahogany Homes when available for lower material burn and more varied Construction training.",
                8, 12, 11, AttentionLevel.MODERATE, 30, 5,
                Arrays.asList("Mahogany Homes access", "Required planks and teleports"), RecommendationConfidence.CHECK_NEEDED);

        addProtected("agility_rooftop", Skill.AGILITY, 1, 99,
                "Best confirmed rooftop or Agility course",
                "Use the highest sensible non-Wilderness course confirmed unlocked. Keep Marks of Grace and Graceful progression in mind rather than rotating away just because a level checkpoint was reached.",
                11, 12, 8, AttentionLevel.ACTIVE, 20, 2,
                Arrays.asList("Confirm the course is unlocked and reachable"), RecommendationConfidence.CHECK_NEEDED);
        addWilderness("agility_wilderness", Skill.AGILITY, 52, 99,
                "Wilderness Agility Course",
                "Use the Wilderness Agility Course only when Wilderness methods are explicitly enabled for this character. Treat PK risk and carried items as separate safety checks.",
                15, 13, 6, AttentionLevel.ACTIVE, 20, 4,
                Arrays.asList("52 Agility", "Wilderness course access"), RecommendationConfidence.CHECK_NEEDED);

        addGeneric("herblore_bank", Skill.HERBLORE, 1, 99,
                "Process confirmed herbs into useful potions",
                "If herbs and secondaries are confirmed available, prioritize potions your account will actually use later.",
                11, 13, 9, AttentionLevel.MODERATE, 10, 3,
                Arrays.asList("Herblore must be unlocked", "Confirm usable herbs and secondaries"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("thieving_best_target", Skill.THIEVING, 1, 99,
                "Best confirmed low-risk Thieving target",
                "Use a stall, chest, or pickpocket target appropriate to your level and food supply, favoring useful loot when efficiency is close.",
                11, 11, 8, AttentionLevel.ACTIVE, 15, 2,
                Arrays.asList("Confirm the target is accessible"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("crafting_banked", Skill.CRAFTING, 1, 99,
                "Process confirmed Crafting supplies",
                "Use gems, hides, glass materials, jewellery supplies, or other resources only after they are observed as available.",
                10, 13, 9, AttentionLevel.MODERATE, 10, 3,
                Arrays.asList("Confirm which Crafting supplies are currently available"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("fletching_logs", Skill.FLETCHING, 1, 99,
                "Fletch confirmed logs into useful products",
                "If logs are confirmed available, favor products that support future Ranged, alching, or money-making plans.",
                9, 12, 12, AttentionLevel.LOW, 10, 1,
                Arrays.asList("Confirm logs or other Fletching supplies"), RecommendationConfidence.CHECK_NEEDED);
        add("slayer_tasks", Skill.SLAYER, 1, 99,
                "Complete Slayer assignments",
                "Use the strongest Slayer master you can access without making tasks unnecessarily slow, and train combat styles that also need levels.",
                12, 14, 9, AttentionLevel.MODERATE, 30, 5,
                Arrays.asList("A suitable Slayer master and task"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("hunter_traps", Skill.HUNTER, 1, 99,
                "Best confirmed Hunter creature or trap",
                "Use the strongest practical Hunter method confirmed unlocked for your level, favoring useful supplies or collection progress when rates are similar.",
                10, 11, 8, AttentionLevel.MODERATE, 15, 5,
                Arrays.asList("Confirm the Hunter method is accessible"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("hunter_birdhouses", Skill.HUNTER, 5, 99,
                "Birdhouse runs",
                "Use birdhouse runs as recurring passive Hunter progress when Fossil Island access and the required supplies are confirmed.",
                13, 15, 14, AttentionLevel.LOW, 5, 5,
                Arrays.asList("Bone Voyage/Fossil Island access", "Birdhouse supplies and transport"), RecommendationConfidence.CHECK_NEEDED);
        add("mining_ore", Skill.MINING, 1, 99,
                "Mine the best useful ore you can access",
                "Choose ore that supports Smithing, Crafting, quests, or money needs when the XP tradeoff is reasonable.",
                10, 12, 9, AttentionLevel.MODERATE, 15, 3, none(), RecommendationConfidence.VERIFIED);
        addProtected("mining_mlm", Skill.MINING, 30, 99,
                "Motherlode Mine",
                "Use Motherlode Mine when available for lower-attention Mining, ores, and Prospector progression. A skill checkpoint should not interrupt the outfit objective.",
                8, 12, 14, AttentionLevel.LOW, 30, 3,
                Arrays.asList("Motherlode Mine access"), RecommendationConfidence.CHECK_NEEDED);
        add("smithing_banked", Skill.SMITHING, 1, 99,
                "Smith confirmed ores and bars",
                "Use ores or bars only after they are observed as available, favouring products that help quests, equipment, ammunition, or later processing.",
                9, 12, 9, AttentionLevel.MODERATE, 10, 3,
                Arrays.asList("Confirm usable ores/bars and furnace or anvil access"), RecommendationConfidence.CHECK_NEEDED);
        addProtected("smithing_foundry", Skill.SMITHING, 15, 99,
                "Giants' Foundry",
                "Use Giants' Foundry when unlocked for material-efficient Smithing and Smiths' Uniform progression.",
                11, 13, 9, AttentionLevel.ACTIVE, 30, 5,
                Arrays.asList("Giants' Foundry access", "Suitable metal supplies"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("fishing_best", Skill.FISHING, 1, 99,
                "Fish the best useful catch currently available",
                "Favor fish your account can cook or use for PvM when XP differences are modest.",
                10, 12, 13, AttentionLevel.LOW, 15, 3, none(), RecommendationConfidence.VERIFIED);
        addProtected("fishing_tempoross", Skill.FISHING, 35, 99,
                "Tempoross",
                "Use Tempoross for active Fishing with useful rewards and collection progression when you want variety.",
                10, 12, 8, AttentionLevel.ACTIVE, 30, 5,
                Arrays.asList("Tempoross access"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("cooking_banked", Skill.COOKING, 1, 99,
                "Cook confirmed raw food",
                "Turn raw food into useful combat supplies only after that food is observed as available.",
                10, 13, 12, AttentionLevel.LOW, 10, 2,
                Arrays.asList("Confirm raw cookable food"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("firemaking_logs", Skill.FIREMAKING, 1, 99,
                "Burn confirmed spare logs",
                "Use spare logs only when they are confirmed available and not reserved for a higher-value goal.",
                9, 9, 8, AttentionLevel.MODERATE, 10, 1,
                Arrays.asList("Confirm logs and a tinderbox"), RecommendationConfidence.CHECK_NEEDED);
        addProtected("firemaking_wintertodt", Skill.FIREMAKING, 50, 99,
                "Wintertodt",
                "Use Wintertodt for Firemaking XP plus supply and collection rewards when your food and access are ready.",
                12, 13, 9, AttentionLevel.ACTIVE, 30, 5,
                Arrays.asList("50 Firemaking", "Wintertodt access", "Suitable food/warm clothing"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("woodcutting_best", Skill.WOODCUTTING, 1, 99,
                "Cut the best useful tree currently available",
                "Favor logs needed for Fletching, Firemaking, Construction, birdhouses, or quests when rates are close.",
                10, 12, 14, AttentionLevel.LOW, 15, 2, none(), RecommendationConfidence.VERIFIED);
        add("woodcutting_forestry", Skill.WOODCUTTING, 1, 99,
                "Forestry-enabled Woodcutting",
                "Train around Forestry activity when available for variety and additional Woodcutting rewards.",
                9, 12, 10, AttentionLevel.MODERATE, 20, 3,
                Arrays.asList("A suitable Forestry area and axe"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("farming_early", Skill.FARMING, 1, 99,
                "Use confirmed Farming patches and seeds",
                "Use only patches with observed access, then source seeds, compost, and tools according to account type.",
                11, 14, 11, AttentionLevel.LOW, 10, 5,
                Arrays.asList("Confirm reachable Farming patches", "Confirm seeds and farming tools"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("farming_herbs", Skill.FARMING, 9, 99,
                "Herb and tree runs",
                "Use the active run checklist to work through reachable herb and tree patches. Each observed planted patch turns complete until it becomes actionable again.",
                13, 15, 13, AttentionLevel.LOW, 10, 5,
                Arrays.asList("9 Farming", "Seeds/saplings", "Tools/Tool Leprechaun state", "Reachable patches"), RecommendationConfidence.CHECK_NEEDED);
        addGeneric("sailing_unlocked", Skill.SAILING, 1, 99,
                "Choose the best confirmed Sailing activity",
                "Only select an exact Sailing activity after confirming its port, activity, and progression unlocks.",
                10, 12, 9, AttentionLevel.MODERATE, 20, 5,
                Arrays.asList("Confirm the Sailing activity and port are unlocked"), RecommendationConfidence.CHECK_NEEDED);
    }

    private void add(String id, Skill skill, int min, int max, String name,
            String instructions, double efficient, double balanced, double relaxed,
            AttentionLevel attention, int session, int setup,
            List<String> requirements, RecommendationConfidence confidence)
    {
        addWithFlags(id, skill, min, max, name, instructions, efficient,
                balanced, relaxed, attention, session, setup, requirements,
                confidence, false, false);
    }

    /**
     * Compatibility-only breadth row whose wording delegates the actual route
     * choice. Production selection uses the concrete expanded catalogs.
     */
    private void addGeneric(String id, Skill skill, int min, int max,
            String name, String instructions, double efficient,
            double balanced, double relaxed, AttentionLevel attention,
            int session, int setup, List<String> requirements,
            RecommendationConfidence confidence)
    {
        addWithFlags(id, skill, min, max, name, instructions, efficient,
                balanced, relaxed, attention, session, setup, requirements,
                confidence, false, false, true);
    }

    private void addProtected(String id, Skill skill, int min, int max, String name,
            String instructions, double efficient, double balanced, double relaxed,
            AttentionLevel attention, int session, int setup,
            List<String> requirements, RecommendationConfidence confidence)
    {
        addWithFlags(id, skill, min, max, name, instructions, efficient,
                balanced, relaxed, attention, session, setup, requirements,
                confidence, false, true);
    }

    private void addWilderness(String id, Skill skill, int min, int max, String name,
            String instructions, double efficient, double balanced, double relaxed,
            AttentionLevel attention, int session, int setup,
            List<String> requirements, RecommendationConfidence confidence)
    {
        addWithFlags(id, skill, min, max, name, instructions, efficient,
                balanced, relaxed, attention, session, setup, requirements,
                confidence, true, false);
    }

    private void addWithFlags(String id, Skill skill, int min, int max, String name,
            String instructions, double efficient, double balanced, double relaxed,
            AttentionLevel attention, int session, int setup,
            List<String> requirements, RecommendationConfidence confidence,
            boolean wilderness, boolean progressionProtected)
    {
        addWithFlags(id, skill, min, max, name, instructions, efficient,
                balanced, relaxed, attention, session, setup, requirements,
                confidence, wilderness, progressionProtected, false);
    }

    private void addWithFlags(String id, Skill skill, int min, int max,
            String name, String instructions, double efficient,
            double balanced, double relaxed, AttentionLevel attention,
            int session, int setup, List<String> requirements,
            RecommendationConfidence confidence, boolean wilderness,
            boolean progressionProtected, boolean delegatesMethodChoice)
    {
        methods.add(new TrainingMethod(
                id, skill, min, max, name, instructions, efficient, balanced,
                relaxed, attention, session, setup, requirements, confidence,
                false, wilderness, progressionProtected,
                delegatesMethodChoice));
    }

    private static List<String> none()
    {
        return Collections.emptyList();
    }
}
