package com.udderlywet.osrsstrategist;

import java.util.*;
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

    public Guidance build(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan)
    {
        return build(data, skill, currentLevel, targetLevel,
                trainingPlan, true);
    }

    public Guidance build(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan,
            boolean useGroupStorage)
    {
        Guidance uimBronze = uimF2pBronzeGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimBronze != null) return uimBronze;
        Guidance uimCooking = uimF2pCookingGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimCooking != null) return uimCooking;
        Guidance uimRunecraft = uimF2pRunecraftGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimRunecraft != null) return uimRunecraft;
        Guidance uimThieving = uimThievingGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimThieving != null) return uimThieving;

        Guidance cooking = earlyCookingGuidance(
                data, skill, currentLevel, targetLevel,
                trainingPlan, useGroupStorage);
        if (cooking != null) return cooking;

        Guidance exact = adaptiveGuidance == null
                ? null
                : adaptiveGuidance.build(
                        data, skill, currentLevel, targetLevel,
                        trainingPlan, useGroupStorage);
        if (exact != null) return exact;

        Guidance variable = variableGuidance == null
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

    private static Guidance uimF2pBronzeGuidance(
            GameData data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || skill != Skill.SMITHING || plan == null
                || plan.getMethod() == null
                || !"smithing_f2p_uim_bronze".equals(
                        plan.getMethod().getId())
                || AccountMode.fromTypeCode(
                        data.account().getAccountTypeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        return new Guidance(
                Text.get(662)
                        + targetLevel + ".",
                Text.get(673),
                Text.get(684),
                Text.get(686),
                MethodBankingBehavior.LOCAL_PROCESSING);
    }

    private static Guidance uimF2pCookingGuidance(
            GameData data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || skill != Skill.COOKING || plan == null
                || plan.getMethod() == null
                || !"cooking_f2p_uim_carried_fish".equals(
                        plan.getMethod().getId())
                || AccountMode.fromTypeCode(
                        data.account().getAccountTypeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        return new Guidance(
                Text.get(687)
                        + targetLevel + ".",
                Text.get(688),
                Text.get(689),
                Text.get(690),
                MethodBankingBehavior.LOCAL_PROCESSING);
    }

    private static Guidance uimF2pRunecraftGuidance(
            GameData data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || skill != Skill.RUNECRAFT || plan == null
                || plan.getMethod() == null
                || !"runecraft_f2p_uim_local".equals(
                        plan.getMethod().getId())
                || AccountMode.fromTypeCode(
                        data.account().getAccountTypeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        int level = data.account().getSkillLevel(Skill.RUNECRAFT);
        String rune = level >= 20 ? "body" : level >= 14 ? "fire"
                : level >= 9 ? "earth" : level >= 5 ? "water"
                : level >= 2 ? "mind" : "air";
        String altar = level >= 20 ? Text.get(691)
                : level >= 14 ? Text.get(1440)
                : level >= 9 ? Text.get(1441)
                : level >= 5 ? Text.get(1442)
                : level >= 2 ? Text.get(1443)
                : Text.get(1444);
        return new Guidance(
                Text.get(663)
                        + altar + ", craft " + rune
                        + Text.get(664)
                        + targetLevel + ".",
                "Bring the " + rune + Text.get(1285) + rune
                        + Text.get(665),
                Text.get(666) + altar + ".",
                Text.get(667),
                MethodBankingBehavior.LOCAL_PROCESSING);
    }

    private static Guidance uimThievingGuidance(
            GameData data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || skill != Skill.THIEVING || plan == null
                || plan.getMethod() == null
                || AccountMode.fromTypeCode(
                        data.account().getAccountTypeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        String id = plan.getMethod().getId();
        if ("thieving_uim_lumbridge_people".equals(id))
            return new Guidance(
                    Text.get(668)
                            + targetLevel + ".",
                    Text.get(669),
                    Text.get(670),
                    Text.get(671),
                    MethodBankingBehavior.NONE);
        if ("thieving_uim_fruit_stalls".equals(id))
            return new Guidance(
                    Text.get(672)
                            + targetLevel + ".",
                    Text.get(674),
                    Text.get(675),
                    Text.get(676),
                    MethodBankingBehavior.NONE);
        return null;
    }

    private Guidance earlyCookingGuidance(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan,
            boolean useGroupStorage)
    {
        if (data == null
                || data.account() == null
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
                data.account(), currentLevel, targetLevel);
        if (stages.isEmpty()) return null;

        String action = actionGuidance(stages);
        String supplies = supplyGuidance(
                data, data.account(), stages, useGroupStorage);
        String location = locationGuidance(data.quests());
        String note = Text.get(677);

        return new Guidance(
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
            GameData data,
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
                        data.inventory() == null
                                ? null : data.inventory().getItems(),
                        stage.stage.rawItemName);
                int storage = quantityByNameSafeUimStorage(
                        data.storage(), stage.stage.rawItemName);
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
                    .append(Text.get(1445))
                    .append(joinNatural(ownedParts)).append(".");
            if (missingParts.isEmpty())
            {
                text.append(Text.get(678));
            }
            else
            {
                text.append(" Acquire ")
                        .append(joinNatural(missingParts))
                        .append(Text.get(679));
            }
            return text.toString();
        }

        if (data.bank() == null)
        {
            return "Plan for " + requiredSummary(stages)
                    + Text.get(680);
        }

        List<String> ownedParts = new ArrayList<>();
        List<String> missingParts = new ArrayList<>();
        for (StagePlan stage : stages)
        {
            int inventoryQuantity = quantityByName(
                    data.inventory() == null
                            ? null : data.inventory().getItems(),
                    stage.stage.rawItemName);
            int bankQuantity = quantityByName(
                    data.bank().getItems(), stage.stage.rawItemName);
            int groupQuantity = 0;
            if (useGroupStorage && mode.isGroupIronman()
                    && data.groupStorage() != null
                    && data.groupStorage().isObserved())
            {
                groupQuantity = quantityByName(
                        data.groupStorage().getItems(),
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
            text.append(Text.get(681));
            return text.toString();
        }

        if (mode.usesGrandExchange())
        {
            text.append(" Buy ").append(joinNatural(missingParts))
                    .append(Text.get(1446));
        }
        else if (mode.isGroupIronman())
        {
            text.append(" Source ").append(joinNatural(missingParts))
                    .append(useGroupStorage
                            ? Text.get(1447)
                            : ".");
        }
        else
        {
            text.append(" Source ")
                    .append(joinNatural(missingParts))
                    .append(Text.get(682));
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
            return Text.get(683);
        }

        return Text.get(685);
    }

    private static int quantityByName(
            List<ItemState> items,
            String expectedName)
    {
        if (items == null || expectedName == null) return 0;
        int total = 0;
        for (ItemState item : items)
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
        for (Map.Entry<StorageCapability, List<ItemState>> entry
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
