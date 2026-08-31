package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Surfaces observed near-complete Collection Log categories without guessing drops. */
@Singleton
public class CollectionLogCandidateProvider implements StrategyCandidateProvider
{
    @Override
    public String getId() { return "collection-log-candidates"; }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
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

            result.add(new Recommendation(
                    id,
                    "Collection Log: " + category,
                    complete + "/" + total + " entries are observed complete ("
                            + missing + Text.get(200),
                    score,
                    RecommendationConfidence.CHECK_NEEDED,
                    null,
                    CandidateSafetyEvidence.unknown()
            ));
        }

        result.sort(Comparator.comparingDouble(Recommendation::getScore).reversed());
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
