package com.udderlywet.osrsstrategist;

import java.util.List;
import java.util.Locale;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Detects when the player naturally completes the currently suggested skill
 * checkpoint, even if no feedback button was ever pressed.
 */
@Singleton
public class MilestoneTracker
{
    public MilestoneCompletion detectCompletion(
            TrackedMilestone tracked,
            AccountSnapshot account)
    {
        if (tracked == null || account == null)
        {
            return null;
        }

        Skill skill = tracked.getSkill();
        if (skill == null)
        {
            return null;
        }

        int currentLevel = account.getSkillLevel(skill);
        if (currentLevel < tracked.getTargetLevel())
        {
            return null;
        }

        return new MilestoneCompletion(
                tracked.getActivityId(),
                tracked.getTitle(),
                skill,
                tracked.getStartedAtLevel(),
                tracked.getTargetLevel()
        );
    }

    public TrackedMilestone fromRecommendations(
            List<Recommendation> recommendations)
    {
        if (recommendations == null || recommendations.isEmpty())
        {
            return null;
        }

        Recommendation best = recommendations.get(0);
        Skill skill = skillFor(best);

        if (skill == null
                || best.getCurrentLevel() <= 0
                || best.getTargetLevel() <= best.getCurrentLevel())
        {
            return null;
        }

        return new TrackedMilestone(
                best.getId(),
                best.getTitle(),
                skill.name(),
                best.getCurrentLevel(),
                best.getTargetLevel()
        );
    }

    public boolean sameCheckpoint(
            TrackedMilestone first,
            TrackedMilestone second)
    {
        if (first == null || second == null)
        {
            return first == second;
        }

        return safeEquals(first.getActivityId(), second.getActivityId())
                && first.getTargetLevel() == second.getTargetLevel();
    }

    public static Skill skillFor(Recommendation recommendation)
    {
        if (recommendation == null || recommendation.getId() == null)
        {
            return null;
        }

        String prefix = "skill:";
        if (!recommendation.getId().startsWith(prefix))
        {
            return null;
        }

        String name = recommendation.getId()
                .substring(prefix.length())
                .toUpperCase(Locale.ROOT);

        try
        {
            return Skill.valueOf(name);
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }

    private static boolean safeEquals(String first, String second)
    {
        return first == null ? second == null : first.equals(second);
    }
}
