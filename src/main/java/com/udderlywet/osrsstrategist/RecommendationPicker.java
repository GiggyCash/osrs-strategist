package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/**
 * Final chooser for Pick for Me / Surprise Me behavior.
 * Surprise never samples the whole activity database; it stays within a narrow
 * useful score band so variety cannot intentionally select bad progression.
 */
@Singleton
public class RecommendationPicker
{
    private static final double SURPRISE_SCORE_WINDOW = 12.0;
    private static final int MAX_SURPRISE_POOL = 3;

    public Recommendation pick(
            List<Recommendation> recommendations,
            RecommendationSelectionMode mode,
            int entropy)
    {
        if (recommendations == null || recommendations.isEmpty()) return null;

        List<Recommendation> ordered = new ArrayList<>(recommendations);
        ordered.sort(Comparator.comparingDouble(
                Recommendation::getScore).reversed());

        if (mode != RecommendationSelectionMode.SURPRISE)
        {
            return ordered.get(0);
        }

        double bestScore = ordered.get(0).getScore();
        List<Recommendation> pool = new ArrayList<>();
        for (Recommendation recommendation : ordered)
        {
            if (pool.size() >= MAX_SURPRISE_POOL) break;
            if (bestScore - recommendation.getScore() <= SURPRISE_SCORE_WINDOW)
            {
                pool.add(recommendation);
            }
        }

        if (pool.isEmpty()) return ordered.get(0);
        int index = Math.floorMod(entropy, pool.size());
        return pool.get(index);
    }
}
