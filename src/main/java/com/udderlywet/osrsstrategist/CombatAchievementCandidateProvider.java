package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

/**
 * Progresses Combat Achievement rewards by points rather than demanding that
 * every task in a difficulty tier be completed first.
 */
@Singleton
public class CombatAchievementCandidateProvider implements StrategyCandidateProvider
{
    @Override
    public String getId()
    {
        return "combat-achievements";
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

        CombatAchievementSnapshot snapshot = context.getData().getCombatAchievements();
        int points = snapshot.getEarnedPoints();
        for (CombatAchievementTier tier : CombatAchievementTier.values())
        {
            if (points >= tier.getPointsRequired()) continue;
            int remaining = tier.getPointsRequired() - points;
            double score = Math.max(7.0, 20.0 - remaining / 80.0);

            // Strategist should chase accessible points rather than prematurely
            // forcing difficult mechanics simply because the next tier exists.
            result.add(new StrategyCandidate(
                    "ca:" + tier.name().toLowerCase(),
                    "Combat Achievements: " + pretty(tier.name()),
                    points + "/" + tier.getPointsRequired()
                            + " points. Pick the easiest realistically-ready tasks/boss learning opportunities worth the remaining "
                            + remaining + " points.",
                    score,
                    RecommendationConfidence.CHECK_NEEDED));
            break;
        }
        return result;
    }

    private static String pretty(String value)
    {
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
