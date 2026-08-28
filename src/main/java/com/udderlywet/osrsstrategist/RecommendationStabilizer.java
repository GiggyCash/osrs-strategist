package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;

/** Keeps a still-valid plan steady across low-signal account refreshes. */
final class RecommendationStabilizer
{
    private static final double MAX_SCORE_DEFICIT = 5.0;
    private final RecommendationActionabilityPolicy actionabilityPolicy =
            new RecommendationActionabilityPolicy();

    StrategyResult stabilize(List<Recommendation> previous, StrategyResult fresh)
    {
        if (fresh == null || fresh.getRecommendations().isEmpty()
                || previous == null || previous.isEmpty()) return fresh;

        Recommendation oldTop = previous.get(0);
        if (oldTop == null || FallbackRecommendationFactory.isFallback(oldTop))
            return fresh;

        List<Recommendation> current = fresh.getRecommendations();
        Recommendation stillValid = null;
        for (Recommendation candidate : current)
        {
            if (sameCheckpoint(oldTop, candidate))
            {
                stillValid = candidate;
                break;
            }
        }
        // A formerly executable plan may remain in the fresh queue as a
        // secondary preparation card after an item, quest, membership, or
        // access change. Never promote that alternative back into DO NEXT.
        if (stillValid == null
                || !actionabilityPolicy.canLeadQueue(stillValid)
                || stillValid == current.get(0)) return fresh;
        if (current.get(0).getScore() - stillValid.getScore()
                > MAX_SCORE_DEFICIT) return fresh;

        List<Recommendation> stable = new ArrayList<>(current.size());
        stable.add(stillValid);
        for (Recommendation candidate : current)
            if (candidate != stillValid) stable.add(candidate);
        return new StrategyResult(stable, fresh.getOpportunities(),
                fresh.getSignals(), fresh.getPlan());
    }

    private static boolean sameCheckpoint(
            Recommendation previous, Recommendation current)
    {
        if (previous == null || current == null
                || previous.getId() == null
                || !previous.getId().equals(current.getId())) return false;
        return previous.getTargetLevel() == current.getTargetLevel()
                && safe(previous.getTitle()).equals(safe(current.getTitle()));
    }

    private static String safe(String value)
    {
        return value == null ? "" : value;
    }
}
