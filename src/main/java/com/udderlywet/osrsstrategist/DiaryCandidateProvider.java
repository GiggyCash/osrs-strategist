package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Surfaces observed incomplete Achievement Diary regions without guessing tasks. */
@Singleton
public class DiaryCandidateProvider implements StrategyCandidateProvider
{
    private final DiaryKnowledgeCatalog catalog;

    @Inject
    public DiaryCandidateProvider(DiaryKnowledgeCatalog catalog)
    {
        this.catalog = catalog;
    }

    @Override
    public String getId()
    {
        return "diaries";
    }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getDiaries() == null)
        {
            return result;
        }

        DiarySnapshot diaries = context.getData().getDiaries();
        for (String region : catalog.regions())
        {
            int total = diaries.totalIn(region);
            int complete = diaries.completedIn(region);
            if (total <= 0 || complete >= total) continue;

            double score = catalog.scoreForProgress(complete, total);
            if ("wilderness".equals(region))
            {
                if (!context.isAllowWildernessMethods()) continue;
                if (context.getAccountMode() == AccountMode.HARDCORE_IRONMAN
                        || context.getAccountMode() == AccountMode.HARDCORE_GROUP_IRONMAN)
                {
                    continue;
                }
            }

            result.add(new StrategyCandidate(
                    "diary:" + region,
                    "Diary: " + pretty(region),
                    complete + "/" + total
                            + " observed tasks complete. Finish the next verified achievable task in this region; exact task routing stays gated by observed diary state.",
                    score,
                    RecommendationConfidence.VERIFIED));
        }
        return result;
    }

    private static String pretty(String value)
    {
        String[] words = value.split(" ");
        StringBuilder out = new StringBuilder();
        for (String word : words)
        {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
