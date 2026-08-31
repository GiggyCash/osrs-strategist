package compass;

import java.util.*;
import javax.inject.Singleton;

/** Surfaces observed near-complete Collection Log categories without guessing drops. */
@Singleton
public class CollectionLogCandidateProvider implements CandidateProvider
{
    @Override
    public String getId() { return "collection-log-candidates"; }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().collectionLog() == null) return result;

        var log = context.data().collectionLog();
        Set<String> categories = new HashSet<>(log.getCategoryTotals().keySet());
        categories.addAll(log.getCategoryCompleted().keySet());
        for (String category : categories)
        {
            var total = log.getCategoryTotal(category);
            var complete = log.getCategoryCompleted(category);
            if (total <= 0 || complete < 0 || complete >= total) continue;
            var missing = total - complete;
            if (!context.collectionist() && missing > 3) continue;

            var id = "collection-log:" + slug(category);
            if (context.preferenceProfile().isOnCooldown(id)) continue;
            var percent = complete * 100.0 / total;
            var score = 20.0 + Math.min(20.0, percent * 0.20);
            if (missing == 1) score += 14.0;
            else if (missing == 2) score += 9.0;
            else if (missing == 3) score += 5.0;
            if (context.collectionist()) score += 9.0;
            score += context.preferenceProfile().weightFor(id) * 10.0;

            result.add(new Recommendation(
                    id,
                    "Collection Log: " + category,
                    complete + "/" + total + Text.get(1369)
                            + missing + Text.get(200),
                    score,
                    Confidence.CHECK_NEEDED,
                    null,
                    SafetyEvidence.unknown()
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
