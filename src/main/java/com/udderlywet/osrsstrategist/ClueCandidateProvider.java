package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Lets an observed clue become actual DO NEXT work without making it spammy. */
@Singleton
public class ClueCandidateProvider implements StrategyCandidateProvider
{
    private final CluePreparationCatalog preparationCatalog;

    @Inject
    public ClueCandidateProvider(CluePreparationCatalog preparationCatalog)
    {
        this.preparationCatalog = preparationCatalog;
    }

    /** Compatibility constructor for older tests. */
    public ClueCandidateProvider()
    {
        this(new CluePreparationCatalog());
    }

    @Override
    public String getId()
    {
        return "clue-candidates";
    }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null) return result;
        ClueSnapshot clue = context.getData().getClue();
        if (clue == null || !clue.isCluePresent()) return result;

        String id = "clue:pending";
        PreferenceProfile preferences = context.getPreferenceProfile();
        if (preferences.isOnCooldown(id)) return result;

        long age = Math.max(0L,
                System.currentTimeMillis() - clue.getFirstSeenAtMillis());
        double ageHours = age / 3_600_000.0;
        double score = 42.0 + Math.min(15.0, ageHours * 0.5)
                + preferences.weightFor(id) * 10.0;

        String type = clue.getClueType() == null
                ? "clue"
                : clue.getClueType() + " clue";
        List<String> prep = preparationCatalog.preparationFor(clue.getClueType());
        StringBuilder reason = new StringBuilder(
                "Clears the pending clue slot and can advance Collection Log progress. Prep: ");
        for (int i = 0; i < Math.min(3, prep.size()); i++)
        {
            if (i > 0) reason.append(", ");
            reason.append(prep.get(i));
        }
        reason.append(".");

        if (context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN)
        {
            score -= 2.0;
            reason.append(" UIM: verify inventory space and only use STASH/POH storage that Strategist has actually confirmed.");
        }
        if (context.isCollectionistMode()) score += 6.0;

        result.add(new StrategyCandidate(
                id,
                "Complete " + type,
                reason.toString(),
                score,
                clue.getConfidence()
        ));
        return result;
    }
}
