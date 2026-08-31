package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Retains a valid goal step across minor score/inventory refreshes. */
@Singleton
public final class PlanContinuityService
{
    public StrategicPlan reconcile(
            StrategicPlan previous,
            StrategicPlan rebuilt,
            StrategyContext context,
            List<Recommendation> currentRecommendations)
    {
        if (previous == null || !previous.matchesContext(context))
            return rebuilt;

        StrategicPlan advanced = previous.advanceCompleted(
                context == null ? null : context.getData());
        StrategicPlanStep current = advanced.getCurrentStep();
        Set<String> recommendationIds = recommendationIds(
                currentRecommendations);

        // A current executable action becoming illegal, blocked or absent is a
        // material invalidation. Dependency-only future steps may remain while
        // the rebuilt plan supplies their newly executable recommendation.
        String currentRecommendation = current.getRecommendationId();
        if (currentRecommendation != null
                && !recommendationIds.contains(currentRecommendation))
            return rebuilt;

        if (rebuilt == null) return advanced;
        if (current.getId().equals(rebuilt.getCurrentStep().getId()))
            return advanced;

        // Completing an intermediate target deliberately moves to the rebuilt
        // next action. Ordinary score movement does not replace unfinished work.
        if (advanced.getCurrentIndex() > previous.getCurrentIndex())
            return rebuilt;
        return advanced;
    }

    private static Set<String> recommendationIds(
            List<Recommendation> recommendations)
    {
        if (recommendations == null) recommendations = Collections.emptyList();
        Set<String> result = new HashSet<>();
        for (Recommendation recommendation : recommendations)
            if (recommendation != null && recommendation.getId() != null)
                result.add(recommendation.getId());
        return result;
    }
}
