package com.udderlywet.osrsstrategist;

import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** Builds concrete, account-aware instructions for supported recommendations. */
@Singleton
public class RecommendationGuidanceService
{
    private static final String LOW_LEVEL_FISH_METHOD = "cooking_f2p_fish";
    private static final String RAW_TROUT = "Raw trout";
    private static final int TROUT_XP = 70;

    /**
     * Low-level fish have a high burn rate. Until Strategist has a verified
     * location-specific burn model, use a conservative planning buffer so the
     * recommendation does not under-buy supplies and strand the player short of
     * the milestone.
     */
    private static final double LOW_LEVEL_BURN_BUFFER = 2.5;

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

        // This first exact route covers the F2P 15-20 trout band. Other Cooking
        // bands can plug into the same structure as their burn/source models are
        // curated and tested.
        if (currentLevel < 15 || currentLevel >= 20 || targetLevel > 20)
        {
            return null;
        }

        AccountSnapshot account = data.getAccount();
        int currentXp = account.getSkillExperience(Skill.COOKING);
        if (currentXp <= 0)
        {
            currentXp = Experience.getXpForLevel(currentLevel);
        }

        int targetXp = Experience.getXpForLevel(targetLevel);
        int xpNeeded = Math.max(0, targetXp - currentXp);
        int successfulCooks = divideRoundUp(xpNeeded, TROUT_XP);
        int rawNeeded = Math.max(
                successfulCooks,
                (int) Math.ceil(successfulCooks * LOW_LEVEL_BURN_BUFFER)
        );

        String action = "Cook trout to level " + targetLevel
                + ". You need " + successfulCooks
                + " successful trout cook"
                + (successfulCooks == 1 ? "" : "s")
                + " from your current XP.";

        String supplies = supplyGuidance(data, account, rawNeeded);
        String location = locationGuidance(data.getQuests());
        String note = "The raw-trout total includes a conservative low-level "
                + "burn buffer. Burns are random, so you may finish with some "
                + "raw trout left over.";

        return new RecommendationGuidance(
                action,
                supplies,
                location,
                note
        );
    }

    private static String supplyGuidance(
            StrategyDataBundle data,
            AccountSnapshot account,
            int rawNeeded)
    {
        int inventoryQuantity = quantityByName(
                data.getInventory() == null
                        ? null
                        : data.getInventory().getItems(),
                RAW_TROUT
        );

        if (data.getBank() == null)
        {
            return "Plan for about " + rawNeeded + " raw trout. Open your bank "
                    + "once so Strategist can verify your stored supply before "
                    + "telling you exactly how many more to get.";
        }

        int bankQuantity = quantityByName(
                data.getBank().getItems(),
                RAW_TROUT
        );
        int verifiedQuantity = bankQuantity + inventoryQuantity;
        int missing = Math.max(0, rawNeeded - verifiedQuantity);

        if (missing == 0)
        {
            return "Plan for about " + rawNeeded + " raw trout. You have "
                    + verifiedQuantity
                    + " verified across your bank and inventory, so you already "
                    + "have enough for this milestone.";
        }

        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        if (mode.usesGrandExchange())
        {
            return "Plan for about " + rawNeeded + " raw trout. You have "
                    + verifiedQuantity + " verified, so buy " + missing
                    + " raw trout at the Grand Exchange.";
        }

        return "Plan for about " + rawNeeded + " raw trout. You have "
                + verifiedQuantity + " verified, so catch or otherwise source "
                + missing + " more raw trout.";
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

    private static int divideRoundUp(int numerator, int denominator)
    {
        if (numerator <= 0)
        {
            return 0;
        }
        return (numerator + denominator - 1) / denominator;
    }
}
