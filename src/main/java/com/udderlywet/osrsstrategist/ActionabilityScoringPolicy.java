package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

/**
 * Small score adjustment for how immediately actionable a recommendation is.
 *
 * <p>This policy is intentionally bounded. Readiness is useful tie-breaking
 * information, not a replacement for progression value. A fully verified action
 * gets a modest bonus; unresolved preparation gets a modest penalty. A 10-point
 * strategic advantage should not disappear because the stronger route needs the
 * player to confirm one teleport or open the bank once.</p>
 */
@Singleton
public class ActionabilityScoringPolicy
{
    private static final double VERIFIED_BONUS = 2.5;
    private static final double CHECK_BASE_PENALTY = 0.5;
    private static final double EACH_UNRESOLVED_PENALTY = 0.65;
    private static final double MAX_CHECK_PENALTY = 3.5;

    public List<Recommendation> adjust(List<Recommendation> recommendations)
    {
        List<Recommendation> result = new ArrayList<>();
        if (recommendations == null) return result;
        for (Recommendation recommendation : recommendations)
        {
            if (recommendation == null
                    || recommendation.getConfidence() == RecommendationConfidence.BLOCKED)
                continue;
            result.add(copyWithScore(
                    recommendation,
                    recommendation.getScore() + adjustmentFor(recommendation)));
        }
        return result;
    }

    public double adjustmentFor(Recommendation recommendation)
    {
        if (recommendation == null) return 0.0;
        if (recommendation.getConfidence() == RecommendationConfidence.VERIFIED)
            return VERIFIED_BONUS;
        if (recommendation.getConfidence() == RecommendationConfidence.BLOCKED)
            return -1000.0;

        int unresolved = unresolvedRequirements(recommendation);
        double penalty = CHECK_BASE_PENALTY
                + unresolved * EACH_UNRESOLVED_PENALTY;
        return -Math.min(MAX_CHECK_PENALTY, penalty);
    }

    private static int unresolvedRequirements(Recommendation recommendation)
    {
        TrainingPlan plan = recommendation.getTrainingPlan();
        if (plan == null) return 1;
        int unresolved = 0;
        for (RequirementCheck check : plan.getRequirementChecks())
        {
            if (check.getState() != RequirementState.VERIFIED) unresolved++;
        }
        return Math.max(1, unresolved);
    }

    private static Recommendation copyWithScore(
            Recommendation source,
            double score)
    {
        return new Recommendation(
                source.getId(),
                source.getTitle(),
                source.getReason(),
                score,
                source.getTrainingPlan(),
                source.getConfidence(),
                source.getCurrentLevel(),
                source.getTargetLevel());
    }
}
