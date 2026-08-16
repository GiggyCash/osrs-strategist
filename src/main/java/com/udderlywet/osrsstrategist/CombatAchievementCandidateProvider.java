package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

/** Surfaces the next Combat Achievement reward tier without inventing tasks. */
@Singleton
public class CombatAchievementCandidateProvider implements StrategyCandidateProvider
{
    @Override
    public String getId()
    {
        return "combat-achievement-candidates";
    }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getCombatAchievements() == null)
        {
            return result;
        }

        CombatAchievementSnapshot snapshot =
                context.getData().getCombatAchievements();
        CombatAchievementTier next = snapshot.nextRewardTier();
        if (next == null) return result;

        String id = "combat-achievements:" + next.name().toLowerCase();
        if (context.getPreferenceProfile().isOnCooldown(id)) return result;

        int gap = Math.max(0, next.getRewardPoints() - snapshot.getEarnedPoints());
        double score = 26.0;
        if (gap <= 20) score += 17.0;
        else if (gap <= 75) score += 10.0;
        else if (gap <= 200) score += 5.0;
        if (context.getActiveGoal() == GoalType.ELITE_COMBAT_ACHIEVEMENTS)
            score += next.ordinal() <= CombatAchievementTier.ELITE.ordinal() ? 25.0 : 8.0;
        score += context.getPreferenceProfile().weightFor(id) * 10.0;

        result.add(new StrategyCandidate(
                id,
                "Combat Achievements: " + pretty(next.name()),
                "Next reward tier requires " + next.getRewardPoints()
                        + " points. Strategist should prefer realistic tasks on bosses the account is already ready to fight instead of forcing mechanically extreme tasks early.",
                score,
                RecommendationConfidence.CHECK_NEEDED
        ));
        return result;
    }

    private static String pretty(String value)
    {
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
