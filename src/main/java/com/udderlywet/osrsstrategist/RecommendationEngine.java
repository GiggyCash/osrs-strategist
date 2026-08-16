package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

@Singleton
public class RecommendationEngine
{
    private final TrainingMethodSelector trainingMethodSelector;

    @Inject
    public RecommendationEngine(TrainingMethodSelector trainingMethodSelector)
    {
        this.trainingMethodSelector = trainingMethodSelector;
    }

    public List<Recommendation> recommend(
            AccountSnapshot snapshot,
            StrategyMode strategyMode,
            PreferenceProfile preferenceProfile)
    {
        return recommend(StrategyDataBundle.builder(snapshot).build(),
                strategyMode, SessionIntent.PICK_FOR_ME, false,
                preferenceProfile);
    }

    public List<Recommendation> recommend(
            AccountSnapshot snapshot,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        return recommend(StrategyDataBundle.builder(snapshot).build(),
                strategyMode, sessionIntent, false, preferenceProfile);
    }

    public List<Recommendation> recommend(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        return recommend(data, strategyMode, sessionIntent, false,
                preferenceProfile);
    }

    public List<Recommendation> recommend(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile)
    {
        List<Recommendation> recommendations = new ArrayList<>();
        if (data == null || data.getAccount() == null) return recommendations;
        AccountSnapshot snapshot = data.getAccount();

        for (Skill skill : Skill.values())
        {
            int level = snapshot.getSkillLevel(skill);
            if (level >= 99 || skill == Skill.HITPOINTS) continue;
            if (!ContentAccessRules.isSkillAvailable(skill,
                    snapshot.getMembershipStatus())) continue;

            String activityId = "skill:" + skill.name().toLowerCase();
            if (preferenceProfile.isOnCooldown(activityId)) continue;
            int target = nextTarget(level);
            double score = baseScore(skill, level, strategyMode);
            score += preferenceProfile.weightFor(activityId) * 10.0;
            score += preferenceProfile.timedScoreAdjustmentFor(activityId);
            score += milestoneMomentum(level, target);

            TrainingPlan trainingPlan = trainingMethodSelector.select(
                    data, skill, level, strategyMode, sessionIntent,
                    allowWildernessMethods);
            if (trainingPlan != null && trainingPlan.getMethod() != null)
            {
                score += trainingPlan.getMethod().scoreFor(
                        strategyMode, sessionIntent) * 0.35;
            }

            RecommendationConfidence confidence = trainingPlan == null
                    ? RecommendationConfidence.CHECK_NEEDED
                    : trainingPlan.getConfidence();
            recommendations.add(new Recommendation(
                    activityId, "Train " + skill.getName() + " to " + target,
                    skillReason(skill), score, trainingPlan, confidence,
                    level, target));
        }

        recommendations.sort(Comparator.comparingDouble(
                Recommendation::getScore).reversed());
        if (recommendations.size() > 3)
        {
            return new ArrayList<>(recommendations.subList(0, 3));
        }
        return recommendations;
    }

    private double baseScore(Skill skill, int level, StrategyMode strategyMode)
    {
        double score = 20.0;
        if (level < 10) score += 45.0;
        else if (level < 20) score += 35.0;
        else if (level < 30) score += 25.0;
        else if (level < 40) score += 15.0;
        else if (level < 50) score += 8.0;
        score += progressionWeight(skill);
        if (strategyMode == StrategyMode.EFFICIENT) score += efficientBonus(skill);
        else if (strategyMode == StrategyMode.RELAXED) score += relaxedBonus(skill);
        return score;
    }

    private double milestoneMomentum(int level, int target)
    {
        int remaining = target - level;
        if (remaining <= 1) return 8.0;
        if (remaining <= 3) return 4.0;
        return 0.0;
    }

    private double progressionWeight(Skill skill)
    {
        switch (skill)
        {
            case FARMING: return 18.0;
            case HERBLORE: return 17.0;
            case SLAYER: return 14.0;
            case CONSTRUCTION: return 13.0;
            case AGILITY: return 12.0;
            case RUNECRAFT:
            case SAILING: return 11.0;
            case CRAFTING: return 9.0;
            case MAGIC:
            case PRAYER:
            case HUNTER: return 8.0;
            case SMITHING: return 7.0;
            case MINING: return 6.0;
            default: return 4.0;
        }
    }

    private double efficientBonus(Skill skill)
    {
        switch (skill)
        {
            case FARMING:
            case HERBLORE:
            case SLAYER:
            case CONSTRUCTION:
            case AGILITY:
            case RUNECRAFT: return 6.0;
            default: return 0.0;
        }
    }

    private double relaxedBonus(Skill skill)
    {
        switch (skill)
        {
            case FISHING:
            case WOODCUTTING:
            case MINING:
            case COOKING:
            case FLETCHING:
            case FIREMAKING: return 8.0;
            default: return 0.0;
        }
    }

    private int nextTarget(int level)
    {
        if (level < 10) return 10;
        if (level < 20) return 20;
        if (level < 30) return 30;
        if (level < 40) return 40;
        if (level < 50) return 50;
        if (level < 60) return 60;
        if (level < 70) return 70;
        if (level < 80) return 80;
        if (level < 90) return 90;
        return 99;
    }

    private String skillReason(Skill skill)
    {
        switch (skill)
        {
            case FARMING: return "Supports recurring runs, supplies, and later goals.";
            case HERBLORE: return "Unlocks useful potions and supports later PvM.";
            case SLAYER: return "Builds combat while unlocking monsters, drops, and gear.";
            case CONSTRUCTION: return "Builds POH travel, utility, and storage options.";
            case AGILITY: return "Supports shortcuts, Graceful progression, quests, and diaries.";
            case RUNECRAFT: return "Opens rune options and useful training activities.";
            case SAILING: return "Opens ports, voyages, and sea progression.";
            case CRAFTING: return "Supports equipment, jewelry, quests, and upgrades.";
            case MAGIC: return "Improves combat, teleports, and account utility.";
            case PRAYER: return "Unlocks stronger protection and combat prayers.";
            default: return "Useful progress toward broader account goals.";
        }
    }
}
