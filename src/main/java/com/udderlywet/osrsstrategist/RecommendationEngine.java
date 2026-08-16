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
    public RecommendationEngine(
            TrainingMethodSelector trainingMethodSelector)
    {
        this.trainingMethodSelector = trainingMethodSelector;
    }

    public List<Recommendation> recommend(
            AccountSnapshot snapshot,
            StrategyMode strategyMode,
            PreferenceProfile preferenceProfile)
    {
        return recommend(
                snapshot,
                strategyMode,
                SessionIntent.PICK_FOR_ME,
                preferenceProfile
        );
    }

    public List<Recommendation> recommend(
            AccountSnapshot snapshot,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        List<Recommendation> recommendations =
                new ArrayList<>();

        for (Skill skill : Skill.values())
        {
            int level = snapshot.getSkillLevel(skill);

            if (level >= 99)
            {
                continue;
            }

            // Hitpoints is normally progressed through the combat style that
            // the account needs, so do not recommend it as a standalone task.
            if (skill == Skill.HITPOINTS)
            {
                continue;
            }

            String activityId =
                    "skill:" + skill.name().toLowerCase();

            if (preferenceProfile.isOnCooldown(activityId))
            {
                continue;
            }

            int target = nextTarget(level);

            double score =
                    baseScore(skill, level, strategyMode);

            score +=
                    preferenceProfile.weightFor(activityId) * 10.0;

            TrainingPlan trainingPlan =
                    trainingMethodSelector.select(
                            skill,
                            level,
                            strategyMode,
                            sessionIntent
                    );

            if (trainingPlan != null
                    && trainingPlan.getMethod() != null)
            {
                score += trainingPlan.getMethod().scoreFor(
                        strategyMode,
                        sessionIntent
                ) * 0.35;
            }

            String title =
                    "Train " + skill.getName()
                            + " to " + target;

            String reason =
                    buildReason(
                            skill,
                            level,
                            target,
                            trainingPlan
                    );

            RecommendationConfidence confidence =
                    trainingPlan == null
                            ? RecommendationConfidence.CHECK_NEEDED
                            : trainingPlan.getConfidence();

            recommendations.add(
                    new Recommendation(
                            activityId,
                            title,
                            reason,
                            score,
                            trainingPlan,
                            confidence
                    )
            );
        }

        recommendations.sort(
                Comparator.comparingDouble(
                        Recommendation::getScore
                ).reversed()
        );

        if (recommendations.size() > 3)
        {
            return new ArrayList<>(
                    recommendations.subList(0, 3)
            );
        }

        return recommendations;
    }

    private double baseScore(
            Skill skill,
            int level,
            StrategyMode strategyMode)
    {
        double score = 20.0;

        if (level < 10)
        {
            score += 45.0;
        }
        else if (level < 20)
        {
            score += 35.0;
        }
        else if (level < 30)
        {
            score += 25.0;
        }
        else if (level < 40)
        {
            score += 15.0;
        }
        else if (level < 50)
        {
            score += 8.0;
        }

        score += progressionWeight(skill);

        switch (strategyMode)
        {
            case EFFICIENT:
                score += efficientBonus(skill);
                break;

            case RELAXED:
                score += relaxedBonus(skill);
                break;

            case BALANCED:
            default:
                break;
        }

        return score;
    }

    private double progressionWeight(Skill skill)
    {
        switch (skill)
        {
            case FARMING:
                return 18.0;

            case HERBLORE:
                return 17.0;

            case SLAYER:
                return 14.0;

            case CONSTRUCTION:
                return 13.0;

            case AGILITY:
                return 12.0;

            case RUNECRAFT:
                return 11.0;

            case SAILING:
                return 11.0;

            case CRAFTING:
                return 9.0;

            case MAGIC:
                return 8.0;

            case PRAYER:
                return 8.0;

            case HUNTER:
                return 8.0;

            case SMITHING:
                return 7.0;

            case MINING:
                return 6.0;

            default:
                return 4.0;
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
            case RUNECRAFT:
                return 6.0;

            default:
                return 0.0;
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
            case FIREMAKING:
                return 8.0;

            default:
                return 0.0;
        }
    }

    private int nextTarget(int level)
    {
        if (level < 10)
        {
            return 10;
        }
        if (level < 20)
        {
            return 20;
        }
        if (level < 30)
        {
            return 30;
        }
        if (level < 40)
        {
            return 40;
        }
        if (level < 50)
        {
            return 50;
        }
        if (level < 60)
        {
            return 60;
        }
        if (level < 70)
        {
            return 70;
        }
        if (level < 80)
        {
            return 80;
        }
        if (level < 90)
        {
            return 90;
        }
        return 99;
    }

    private String buildReason(
            Skill skill,
            int level,
            int target,
            TrainingPlan trainingPlan)
    {
        StringBuilder reason = new StringBuilder();

        reason.append("Current level: ")
                .append(level)
                .append(". Next checkpoint: ")
                .append(target)
                .append(". ")
                .append(skillReason(skill));

        if (trainingPlan != null
                && trainingPlan.getMethod() != null)
        {
            TrainingMethod method = trainingPlan.getMethod();

            reason.append(" <br><br><b>BEST METHOD:</b> ")
                    .append(method.getName())
                    .append(". ")
                    .append(method.getInstructions())
                    .append(" <br><b>Why:</b> ")
                    .append(trainingPlan.getWhyThisMethod());
        }
        else
        {
            reason.append(" <br><br><b>BEST METHOD:</b> Check needed. Strategist does not yet have a verified method for this account state.");
        }

        return reason.toString();
    }

    private String skillReason(Skill skill)
    {
        switch (skill)
        {
            case FARMING:
                return "Farming supports recurring runs, supplies, and many later account goals.";
            case HERBLORE:
                return "Herblore unlocks stronger potions and becomes important for later progression.";
            case SLAYER:
                return "Slayer progresses combat while opening new monsters, drops, and equipment.";
            case CONSTRUCTION:
                return "Construction develops POH utility, travel, storage, and later account convenience.";
            case AGILITY:
                return "Agility supports shortcuts and many future quest and diary requirements.";
            case RUNECRAFT:
                return "Runecraft opens rune options and future training activities.";
            case SAILING:
                return "Sailing opens sea content and future progression options.";
            case CRAFTING:
                return "Crafting supports equipment, jewelry, quests, and future upgrades.";
            case MAGIC:
                return "Magic improves combat, teleports, utility, and later spell options.";
            case PRAYER:
                return "Prayer unlocks stronger protection and combat prayers.";
            default:
                return "This is a useful step toward stronger overall account progression.";
        }
    }
}
