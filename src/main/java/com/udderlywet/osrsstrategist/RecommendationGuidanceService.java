package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Builds concrete, account-aware instructions for skill recommendations.
 *
 * <p>Guidance resolves in layers: special burn-aware routes, curated exact
 * execution profiles, variable-XP activity planners, then the universal
 * RuneLite action fallback. Every layer refuses to invent data it cannot prove.</p>
 */
@Singleton
public class RecommendationGuidanceService
{
    private static final String LOW_LEVEL_FISH_METHOD = "cooking_f2p_fish";
    private static final double LOW_LEVEL_BURN_BUFFER = 2.5;

    private static final List<CookingStage> F2P_EARLY_COOKING = Arrays.asList(
            new CookingStage(1, 5, "sardine", "Raw sardine", 40),
            new CookingStage(5, 15, "herring", "Raw herring", 50),
            new CookingStage(15, 20, "trout", "Raw trout", 70),
            new CookingStage(20, 25, "pike", "Raw pike", 80),
            new CookingStage(25, 30, "salmon", "Raw salmon", 90)
    );

    private final AdaptiveMilestoneGuidanceService adaptiveGuidance;
    private final VariableMethodGuidanceService variableGuidance;
    private final UniversalSkillActionGuidanceService universalGuidance;

    @Inject
    public RecommendationGuidanceService(
            AdaptiveMilestoneGuidanceService adaptiveGuidance,
            VariableMethodGuidanceService variableGuidance,
            UniversalSkillActionGuidanceService universalGuidance)
    {
        this.adaptiveGuidance = adaptiveGuidance;
        this.variableGuidance = variableGuidance;
        this.universalGuidance = universalGuidance;
    }

    /** Compatibility constructor retained for focused tests and older callers. */
    public RecommendationGuidanceService(
            AdaptiveMilestoneGuidanceService adaptiveGuidance)
    {
        this(adaptiveGuidance,
                new VariableMethodGuidanceService(),
                new UniversalSkillActionGuidanceService());
    }

    /** Compatibility constructor for focused tests and older callers. */
    public RecommendationGuidanceService()
    {
        this(new AdaptiveMilestoneGuidanceService(),
                new VariableMethodGuidanceService(),
                new UniversalSkillActionGuidanceService());
    }

    public RecommendationGuidance build(
            StrategyDataBundle data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan)
    {
        return build(data, skill, currentLevel, targetLevel,
                trainingPlan, true);
    }

    public RecommendationGuidance build(
            StrategyDataBundle data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan,
            boolean useGroupStorage)
    {
        RecommendationGuidance uimBronze = uimF2pBronzeGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimBronze != null) return uimBronze;
        RecommendationGuidance uimCooking = uimF2pCookingGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimCooking != null) return uimCooking;
        RecommendationGuidance uimRunecraft = uimF2pRunecraftGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimRunecraft != null) return uimRunecraft;
        RecommendationGuidance uimThieving = uimThievingGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimThieving != null) return uimThieving;

        RecommendationGuidance cooking = earlyCookingGuidance(
                data, skill, currentLevel, targetLevel,
                trainingPlan, useGroupStorage);
        if (cooking != null) return cooking;

        RecommendationGuidance exact = adaptiveGuidance == null
                ? null
                : adaptiveGuidance.build(
                        data, skill, currentLevel, targetLevel,
                        trainingPlan, useGroupStorage);
        if (exact != null) return exact;

        RecommendationGuidance variable = variableGuidance == null
                ? null
                : variableGuidance.build(
                        data, skill, currentLevel, targetLevel,
                        trainingPlan, useGroupStorage);
        if (variable != null) return variable;

        return universalGuidance == null
                ? null
                : universalGuidance.build(
                        data, skill, currentLevel, targetLevel,
                        trainingPlan, useGroupStorage);
    }

    private static RecommendationGuidance uimF2pBronzeGuidance(
            StrategyDataBundle data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.getAccount() == null
                || skill != Skill.SMITHING || plan == null
                || plan.getMethod() == null
                || !"smithing_f2p_uim_bronze".equals(
                        plan.getMethod().getId())
                || AccountMode.fromTypeCode(
                        data.getAccount().getAccountTypeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        return new RecommendationGuidance(
                "Mine one copper ore and one tin ore, smelt them into a bronze bar, smith the highest bronze item that fits the carried bars, and repeat until Smithing level "
                        + targetLevel + ".",
                "Bring a pickaxe and hammer; buy a hammer from Lumbridge General Store if it is missing.",
                "East Lumbridge Swamp copper and tin rocks, then the furnace and rusted bronze anvil in the Smithing tutor's building north of Lumbridge Castle.",
                "The loop sources and consumes each pair of ores locally, so no conventional bank or stored bar supply is assumed.",
                MethodBankingBehavior.LOCAL_PROCESSING);
    }

    private static RecommendationGuidance uimF2pCookingGuidance(
            StrategyDataBundle data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.getAccount() == null
                || skill != Skill.COOKING || plan == null
                || plan.getMethod() == null
                || !"cooking_f2p_uim_carried_fish".equals(
                        plan.getMethod().getId())
                || AccountMode.fromTypeCode(
                        data.getAccount().getAccountTypeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        return new RecommendationGuidance(
                "Cook the carried raw fish on the permanent fire. If the carried supply runs out first, fly-fish trout and salmon at the adjacent river and cook each inventory until Cooking level "
                        + targetLevel + ".",
                "Bring the carried raw fish. For the resupply loop, bring a fly fishing rod and feathers.",
                "Barbarian Village river and the permanent fire immediately east of the fishing spots.",
                "Inputs become useful food in place, so the loop is viable without conventional banking or an empty inventory.",
                MethodBankingBehavior.LOCAL_PROCESSING);
    }

    private static RecommendationGuidance uimF2pRunecraftGuidance(
            StrategyDataBundle data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.getAccount() == null
                || skill != Skill.RUNECRAFT || plan == null
                || plan.getMethod() == null
                || !"runecraft_f2p_uim_local".equals(
                        plan.getMethod().getId())
                || AccountMode.fromTypeCode(
                        data.getAccount().getAccountTypeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        int level = data.getAccount().getSkillLevel(Skill.RUNECRAFT);
        String rune = level >= 20 ? "body" : level >= 14 ? "fire"
                : level >= 9 ? "earth" : level >= 5 ? "water"
                : level >= 2 ? "mind" : "air";
        String altar = level >= 20 ? "Body Altar south of Edgeville Monastery"
                : level >= 14 ? "Fire Altar north of Al Kharid"
                : level >= 9 ? "Earth Altar northeast of Varrock"
                : level >= 5 ? "Water Altar in Lumbridge Swamp"
                : level >= 2 ? "Mind Altar north of Falador"
                : "Air Altar southwest of Falador";
        return new RecommendationGuidance(
                "Mine one carried inventory of rune essence through Sedridor, walk it to the "
                        + altar + ", craft " + rune
                        + " runes, and repeat until Runecraft level "
                        + targetLevel + ".",
                "Bring the " + rune + " talisman or wear the " + rune
                        + " tiara; leave the remaining slots for rune essence.",
                "Wizards' Tower rune-essence mine, then the " + altar + ".",
                "Essence is mined and consumed one trip at a time; no conventional bank or banked essence is assumed.",
                MethodBankingBehavior.LOCAL_PROCESSING);
    }

    private static RecommendationGuidance uimThievingGuidance(
            StrategyDataBundle data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.getAccount() == null
                || skill != Skill.THIEVING || plan == null
                || plan.getMethod() == null
                || AccountMode.fromTypeCode(
                        data.getAccount().getAccountTypeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        String id = plan.getMethod().getId();
        if ("thieving_uim_lumbridge_people".equals(id))
            return new RecommendationGuidance(
                    "Pickpocket men and women around Lumbridge Castle, stop before another failed pickpocket could be unsafe, ask a monk at Edgeville Monastery to heal you, and repeat until Thieving level "
                            + targetLevel + ".",
                    "No carried supplies are required; preserve the current inventory and do not bank between healing trips.",
                    "Men and women around Lumbridge Castle, with healing from the monks at Edgeville Monastery.",
                    "This is a slow bank-free recovery route for early UIM Thieving; stronger routes can replace it when their access and setup are proven.",
                    MethodBankingBehavior.NONE);
        if ("thieving_uim_fruit_stalls".equals(id))
            return new RecommendationGuidance(
                    "Steal from both fruit stalls, drop only the freshly stolen fruit while moving between them, and repeat until Thieving level "
                            + targetLevel + ".",
                    "No carried supplies are required; preserve every pre-existing setup item.",
                    "The two fruit stalls in the easternmost house near the Hosidius beach.",
                    "This lower-rate fallback stays executable when a stronger UIM Thieving route or healing setup is not proven.",
                    MethodBankingBehavior.NONE);
        return null;
    }

    private RecommendationGuidance earlyCookingGuidance(
            StrategyDataBundle data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan,
            boolean useGroupStorage)
    {
        if (data == null
                || data.getAccount() == null
                || skill != Skill.COOKING
                || trainingPlan == null
                || trainingPlan.getMethod() == null
                || !LOW_LEVEL_FISH_METHOD.equals(trainingPlan.getMethod().getId()))
        {
            return null;
        }

        if (currentLevel < 1 || currentLevel >= 30 || targetLevel > 30)
        {
            return null;
        }

        List<StagePlan> stages = buildStages(
                data.getAccount(), currentLevel, targetLevel);
        if (stages.isEmpty()) return null;

        String action = actionGuidance(stages);
        String supplies = supplyGuidance(
                data, data.getAccount(), stages, useGroupStorage);
        String location = locationGuidance(data.getQuests());
        String note = "Raw-fish totals include a conservative low-level burn buffer. Burns are random, so you may finish a stage with some raw fish left over.";

        return new RecommendationGuidance(
                action, supplies, location, note);
    }

    private static List<StagePlan> buildStages(
            AccountSnapshot account,
            int currentLevel,
            int targetLevel)
    {
        List<StagePlan> plans = new ArrayList<>();
        int currentXp = account.getSkillExperience(Skill.COOKING);
        if (currentXp <= 0)
        {
            currentXp = Experience.getXpForLevel(currentLevel);
        }

        int stageStartXp = currentXp;
        for (CookingStage stage : F2P_EARLY_COOKING)
        {
            if (stage.endLevel <= currentLevel
                    || stage.startLevel >= targetLevel)
            {
                continue;
            }

            int stageTargetLevel = Math.min(stage.endLevel, targetLevel);
            int stageTargetXp = Experience.getXpForLevel(stageTargetLevel);
            int xpNeeded = Math.max(0, stageTargetXp - stageStartXp);
            int successfulCooks = divideRoundUp(xpNeeded, stage.xpEach);
            int rawNeeded = Math.max(
                    successfulCooks,
                    (int) Math.ceil(successfulCooks * LOW_LEVEL_BURN_BUFFER));

            if (successfulCooks > 0)
            {
                plans.add(new StagePlan(
                        stage, stageTargetLevel, successfulCooks, rawNeeded));
            }

            stageStartXp = stageTargetXp;
            if (stageTargetLevel >= targetLevel) break;
        }
        return plans;
    }

    private static String actionGuidance(List<StagePlan> stages)
    {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < stages.size(); i++)
        {
            StagePlan stage = stages.get(i);
            if (i > 0) text.append(" Then ");
            text.append("cook ")
                    .append(stage.stage.foodName)
                    .append(" to level ")
                    .append(stage.targetLevel)
                    .append(" (about ")
                    .append(stage.successfulCooks)
                    .append(" successful cook")
                    .append(stage.successfulCooks == 1 ? "" : "s")
                    .append(").");
        }
        return capitalize(text.toString());
    }

    private static String supplyGuidance(
            StrategyDataBundle data,
            AccountSnapshot account,
            List<StagePlan> stages,
            boolean useGroupStorage)
    {
        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());

        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            List<String> ownedParts = new ArrayList<>();
            List<String> missingParts = new ArrayList<>();
            for (StagePlan stage : stages)
            {
                int inventory = quantityByName(
                        data.getInventory() == null
                                ? null : data.getInventory().getItems(),
                        stage.stage.rawItemName);
                int storage = quantityByNameSafeUimStorage(
                        data.getStorage(), stage.stage.rawItemName);
                int verified = inventory + storage;
                int missing = Math.max(0, stage.rawNeeded - verified);
                ownedParts.add(verified + " "
                        + stage.stage.rawItemName.toLowerCase());
                if (missing > 0)
                {
                    missingParts.add(missing + " "
                            + stage.stage.rawItemName.toLowerCase());
                }
            }

            StringBuilder text = new StringBuilder();
            text.append("Plan for ").append(requiredSummary(stages))
                    .append(". Directly usable UIM supply: ")
                    .append(joinNatural(ownedParts)).append(".");
            if (missingParts.isEmpty())
            {
                text.append(" You already have enough in inventory/verified safe storage.");
            }
            else
            {
                text.append(" Acquire ")
                        .append(joinNatural(missingParts))
                        .append(" just in time: buy sardines at Gerrant's Fishy Business in Port Sarim; catch herring or pike with a fishing rod and bait in Lumbridge; catch trout or salmon with a fly fishing rod and feathers at Barbarian Village. Normal bank state is ignored for UIM.");
            }
            return text.toString();
        }

        if (data.getBank() == null)
        {
            return "Plan for " + requiredSummary(stages)
                    + ". Open your bank once to verify stored fish before calculating the remaining shortfall.";
        }

        List<String> ownedParts = new ArrayList<>();
        List<String> missingParts = new ArrayList<>();
        for (StagePlan stage : stages)
        {
            int inventoryQuantity = quantityByName(
                    data.getInventory() == null
                            ? null : data.getInventory().getItems(),
                    stage.stage.rawItemName);
            int bankQuantity = quantityByName(
                    data.getBank().getItems(), stage.stage.rawItemName);
            int groupQuantity = 0;
            if (useGroupStorage && mode.isGroupIronman()
                    && data.getGroupStorage() != null
                    && data.getGroupStorage().isObserved())
            {
                groupQuantity = quantityByName(
                        data.getGroupStorage().getItems(),
                        stage.stage.rawItemName);
            }
            int verified = bankQuantity + inventoryQuantity + groupQuantity;
            int missing = Math.max(0, stage.rawNeeded - verified);

            ownedParts.add(verified + " "
                    + stage.stage.rawItemName.toLowerCase());
            if (missing > 0)
            {
                missingParts.add(missing + " "
                        + stage.stage.rawItemName.toLowerCase());
            }
        }

        StringBuilder text = new StringBuilder();
        text.append("Plan for ")
                .append(requiredSummary(stages))
                .append(". Verified: ")
                .append(joinNatural(ownedParts))
                .append(".");

        if (missingParts.isEmpty())
        {
            text.append(" You already have enough for this milestone.");
            return text.toString();
        }

        if (mode.usesGrandExchange())
        {
            text.append(" Buy ").append(joinNatural(missingParts))
                    .append(" at the Grand Exchange.");
        }
        else if (mode.isGroupIronman())
        {
            text.append(" Source ").append(joinNatural(missingParts))
                    .append(useGroupStorage
                            ? " after checking usable Group Storage."
                            : ".");
        }
        else
        {
            text.append(" Source ")
                    .append(joinNatural(missingParts))
                    .append(": buy sardines at Gerrant's Fishy Business in Port Sarim; catch herring or pike with a fishing rod and bait in Lumbridge; catch trout or salmon with a fly fishing rod and feathers at Barbarian Village.");
        }
        return text.toString();
    }

    private static String requiredSummary(List<StagePlan> stages)
    {
        List<String> parts = new ArrayList<>();
        for (StagePlan stage : stages)
        {
            parts.add("about " + stage.rawNeeded + " "
                    + stage.stage.rawItemName.toLowerCase());
        }
        return joinNatural(parts);
    }

    private static String locationGuidance(QuestSnapshot quests)
    {
        if (quests != null
                && quests.statusOf("Cook's Assistant") == QuestStatus.COMPLETE)
        {
            return "Use the Lumbridge Castle range to reduce burns, banking upstairs between inventories.";
        }

        return "Use the range immediately north of Al Kharid bank: withdraw raw fish, cook one inventory, bank, and repeat.";
    }

    private static int quantityByName(
            List<ItemStackSnapshot> items,
            String expectedName)
    {
        if (items == null || expectedName == null) return 0;
        int total = 0;
        for (ItemStackSnapshot item : items)
        {
            if (item != null && item.getName() != null
                    && expectedName.equalsIgnoreCase(item.getName()))
            {
                total += Math.max(0, item.getQuantity());
            }
        }
        return total;
    }

    private static int quantityByNameSafeUimStorage(
            StorageSnapshot storage,
            String expectedName)
    {
        if (storage == null || expectedName == null) return 0;
        int total = 0;
        for (Map.Entry<StorageCapability, List<ItemStackSnapshot>> entry
                : storage.getObservedContents().entrySet())
        {
            StorageCapability capability = entry.getKey();
            if (!storage.verified(capability)
                    || UimStorageMechanics.isRestrictedRetrieval(capability))
            {
                continue;
            }
            total += quantityByName(entry.getValue(), expectedName);
        }
        return total;
    }

    private static String joinNatural(List<String> parts)
    {
        if (parts == null || parts.isEmpty()) return "nothing";
        if (parts.size() == 1) return parts.get(0);
        if (parts.size() == 2) return parts.get(0) + " and " + parts.get(1);

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.size(); i++)
        {
            if (i > 0)
            {
                text.append(i == parts.size() - 1 ? ", and " : ", ");
            }
            text.append(parts.get(i));
        }
        return text.toString();
    }

    private static String capitalize(String value)
    {
        if (value == null || value.isEmpty()) return "";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static int divideRoundUp(int numerator, int denominator)
    {
        if (numerator <= 0) return 0;
        return (numerator + denominator - 1) / denominator;
    }

    private static final class CookingStage
    {
        private final int startLevel;
        private final int endLevel;
        private final String foodName;
        private final String rawItemName;
        private final int xpEach;

        private CookingStage(
                int startLevel,
                int endLevel,
                String foodName,
                String rawItemName,
                int xpEach)
        {
            this.startLevel = startLevel;
            this.endLevel = endLevel;
            this.foodName = foodName;
            this.rawItemName = rawItemName;
            this.xpEach = xpEach;
        }
    }

    private static final class StagePlan
    {
        private final CookingStage stage;
        private final int targetLevel;
        private final int successfulCooks;
        private final int rawNeeded;

        private StagePlan(
                CookingStage stage,
                int targetLevel,
                int successfulCooks,
                int rawNeeded)
        {
            this.stage = stage;
            this.targetLevel = targetLevel;
            this.successfulCooks = successfulCooks;
            this.rawNeeded = rawNeeded;
        }
    }
}
