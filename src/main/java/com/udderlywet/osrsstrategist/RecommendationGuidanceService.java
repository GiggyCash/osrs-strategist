package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** Builds concrete, account-aware instructions for supported recommendations. */
@Singleton
public class RecommendationGuidanceService
{
    private static final String LOW_LEVEL_FISH_METHOD = "cooking_f2p_fish";

    /**
     * The early fish route deliberately uses a conservative raw-fish buffer.
     * Burns are random, so Strategist plans enough supply to avoid sending the
     * player back to the bank or Grand Exchange just short of a milestone.
     */
    private static final double LOW_LEVEL_BURN_BUFFER = 2.5;

    private static final List<CookingStage> F2P_EARLY_COOKING = Arrays.asList(
            new CookingStage(1, 5, "sardine", "Raw sardine", 40),
            new CookingStage(5, 15, "herring", "Raw herring", 50),
            new CookingStage(15, 20, "trout", "Raw trout", 70),
            new CookingStage(20, 25, "pike", "Raw pike", 80),
            new CookingStage(25, 30, "salmon", "Raw salmon", 90)
    );

    public RecommendationGuidance build(
            StrategyDataBundle data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan)
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

        // The first curated exact planner covers the full F2P fish route through
        // level 30. Higher bands can use the same structure once their burn and
        // location models are curated instead of falling back to guesses.
        if (currentLevel < 1 || currentLevel >= 30 || targetLevel > 30)
        {
            return null;
        }

        List<StagePlan> stages = buildStages(
                data.getAccount(),
                currentLevel,
                targetLevel
        );
        if (stages.isEmpty())
        {
            return null;
        }

        String action = actionGuidance(stages);
        String supplies = supplyGuidance(data, data.getAccount(), stages);
        String location = locationGuidance(data.getQuests());
        String note = "Raw-fish totals include a conservative low-level burn "
                + "buffer. Burns are random, so you may finish a stage with "
                + "some raw fish left over.";

        return new RecommendationGuidance(
                action,
                supplies,
                location,
                note
        );
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
                    (int) Math.ceil(
                            successfulCooks * LOW_LEVEL_BURN_BUFFER
                    )
            );

            if (successfulCooks > 0)
            {
                plans.add(new StagePlan(
                        stage,
                        stageTargetLevel,
                        successfulCooks,
                        rawNeeded
                ));
            }

            stageStartXp = stageTargetXp;
            if (stageTargetLevel >= targetLevel)
            {
                break;
            }
        }
        return plans;
    }

    private static String actionGuidance(List<StagePlan> stages)
    {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < stages.size(); i++)
        {
            StagePlan stage = stages.get(i);
            if (i > 0)
            {
                text.append(" Then ");
            }
            text.append("cook ")
                    .append(stage.stage.foodName)
                    .append(" to level ")
                    .append(stage.targetLevel)
                    .append(": ")
                    .append(stage.successfulCooks)
                    .append(" successful cook")
                    .append(stage.successfulCooks == 1 ? "" : "s")
                    .append(".");
        }
        return capitalize(text.toString());
    }

    private static String supplyGuidance(
            StrategyDataBundle data,
            AccountSnapshot account,
            List<StagePlan> stages)
    {
        if (data.getBank() == null)
        {
            return "Plan for " + requiredSummary(stages)
                    + ". Open your bank once so Strategist can verify stored "
                    + "fish before telling you exactly how many more to get.";
        }

        List<String> ownedParts = new ArrayList<>();
        List<String> missingParts = new ArrayList<>();
        for (StagePlan stage : stages)
        {
            int inventoryQuantity = quantityByName(
                    data.getInventory() == null
                            ? null
                            : data.getInventory().getItems(),
                    stage.stage.rawItemName
            );
            int bankQuantity = quantityByName(
                    data.getBank().getItems(),
                    stage.stage.rawItemName
            );
            int verified = bankQuantity + inventoryQuantity;
            int missing = Math.max(0, stage.rawNeeded - verified);

            ownedParts.add(
                    verified + " " + stage.stage.rawItemName.toLowerCase()
            );
            if (missing > 0)
            {
                missingParts.add(
                        missing + " " + stage.stage.rawItemName.toLowerCase()
                );
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

        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        if (mode.usesGrandExchange())
        {
            text.append(" Buy ")
                    .append(joinNatural(missingParts))
                    .append(" at the Grand Exchange.");
        }
        else
        {
            text.append(" Catch or otherwise source ")
                    .append(joinNatural(missingParts))
                    .append(".");
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
            return "Use the Lumbridge Castle range to reduce burns, banking "
                    + "upstairs between inventories.";
        }

        return "Use a nearby range. If Cook's Assistant is complete, prefer the "
                + "Lumbridge Castle range for its lower burn rate.";
    }

    private static int quantityByName(
            List<ItemStackSnapshot> items,
            String expectedName)
    {
        if (items == null || expectedName == null)
        {
            return 0;
        }

        int total = 0;
        for (ItemStackSnapshot item : items)
        {
            if (item != null
                    && item.getName() != null
                    && expectedName.equalsIgnoreCase(item.getName()))
            {
                total += Math.max(0, item.getQuantity());
            }
        }
        return total;
    }

    private static String joinNatural(List<String> parts)
    {
        if (parts == null || parts.isEmpty())
        {
            return "nothing";
        }
        if (parts.size() == 1)
        {
            return parts.get(0);
        }
        if (parts.size() == 2)
        {
            return parts.get(0) + " and " + parts.get(1);
        }

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
        if (value == null || value.isEmpty())
        {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static int divideRoundUp(int numerator, int denominator)
    {
        if (numerator <= 0)
        {
            return 0;
        }
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
