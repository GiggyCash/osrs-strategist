package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Singleton;

/** Surfaces observed near-complete Collection Log categories without guessing drops. */
@Singleton
public class CollectionLogCandidateProvider implements StrategyCandidateProvider
{
    @Override
    public String getId() { return "collection-log-candidates"; }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getCollectionLog() == null) return result;

        CollectionLogSnapshot log = context.getData().getCollectionLog();
        Set<String> categories = new HashSet<>(log.getCategoryTotals().keySet());
        categories.addAll(log.getCategoryCompleted().keySet());
        for (String category : categories)
        {
            int total = log.getCategoryTotal(category);
            int complete = log.getCategoryCompleted(category);
            if (total <= 0 || complete < 0 || complete >= total) continue;
            int missing = total - complete;
            if (!context.isCollectionistMode() && missing > 3) continue;

            String id = "collection-log:" + slug(category);
            if (context.getPreferenceProfile().isOnCooldown(id)) continue;
            double percent = complete * 100.0 / total;
            double score = 20.0 + Math.min(20.0, percent * 0.20);
            if (missing == 1) score += 14.0;
            else if (missing == 2) score += 9.0;
            else if (missing == 3) score += 5.0;
            if (context.isCollectionistMode()) score += 9.0;
            score += context.getPreferenceProfile().weightFor(id) * 10.0;

            result.add(new StrategyCandidate(
                    id,
                    "Collection Log: " + category,
                    complete + "/" + total + " entries are observed complete ("
                            + missing + " remaining). Strategist will identify the missing items and their account-appropriate sources before turning this into an exact grind.",
                    score,
                    RecommendationConfidence.CHECK_NEEDED
            ));
        }

        result.sort(Comparator.comparingDouble(StrategyCandidate::getScore).reversed());
        if (result.size() > 3) return new ArrayList<>(result.subList(0, 3));
        return result;
    }

    private static String slug(String value)
    {
        return value == null ? "unknown" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
