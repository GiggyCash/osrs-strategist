package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Surfaces only minigames whose unlock has actually been observed. */
@Singleton
public class MinigameCandidateProvider implements StrategyCandidateProvider
{
    private final MinigameKnowledgeCatalog catalog;

    @Inject
    public MinigameCandidateProvider(MinigameKnowledgeCatalog catalog)
    {
        this.catalog = catalog;
    }

    @Override
    public String getId()
    {
        return "minigames";
    }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getMinigames() == null)
        {
            return result;
        }

        MinigameSnapshot snapshot = context.getData().getMinigames();
        for (MinigameKnowledgeDefinition definition : catalog.all())
        {
            if (!snapshot.isUnlocked(definition.getId())) continue;
            String id = "minigame:" + definition.getId();
            if (context.getPreferenceProfile().isOnCooldown(id)) continue;
            double score = definition.getScore()
                    + context.getPreferenceProfile().weightFor(id) * 10.0;
            result.add(new StrategyCandidate(
                    id,
                    definition.getName(),
                    definition.getPurpose(),
                    score,
                    RecommendationConfidence.VERIFIED));
        }
        return result;
    }
}
