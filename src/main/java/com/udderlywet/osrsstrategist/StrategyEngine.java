package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Top-level strategist coordinator. */
@Singleton
public class StrategyEngine
{
    private final RecommendationEngine recommendationEngine;
    private final OpportunityEngine opportunityEngine;
    private final StrategyModuleRegistry moduleRegistry;
    private final StrategyCandidateRegistry candidateRegistry;

    @Inject
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry,
            StrategyCandidateRegistry candidateRegistry)
    {
        this.recommendationEngine = recommendationEngine;
        this.opportunityEngine = opportunityEngine;
        this.moduleRegistry = moduleRegistry;
        this.candidateRegistry = candidateRegistry;
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry)
    {
        this(recommendationEngine, opportunityEngine, moduleRegistry, null);
    }

    public StrategyResult evaluate(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        return evaluate(data, strategyMode, sessionIntent,
                QuestTolerance.NORMAL, GoalType.MAX,
                true, false, false, preferenceProfile);
    }

    public StrategyResult evaluate(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            PreferenceProfile preferenceProfile)
    {
        return evaluate(data, strategyMode, sessionIntent, questTolerance,
                activeGoal, useGroupStorage, collectionistMode, false,
                preferenceProfile);
    }

    public StrategyResult evaluate(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile)
    {
        if (data == null || data.getAccount() == null)
        {
            return new StrategyResult(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList());
        }

        StrategyContext context = new StrategyContext(
                data, strategyMode, sessionIntent, questTolerance, activeGoal,
                useGroupStorage, collectionistMode, allowWildernessMethods,
                preferenceProfile);

        // Skill training is one candidate source, not the entire final product.
        // Generic providers let verified quests, PvM, diaries, gear steps, clues,
        // minigames, and future content compete for DO NEXT without rewriting
        // the recommendation engine.
        List<Recommendation> recommendations = new ArrayList<>(
                recommendationEngine.recommend(
                        data,
                        context.getStrategyMode(),
                        context.getSessionIntent(),
                        context.isUseGroupStorage(),
                        context.isAllowWildernessMethods(),
                        context.getPreferenceProfile()));

        if (candidateRegistry != null)
        {
            for (StrategyCandidateProvider provider
                    : candidateRegistry.getProviders())
            {
                List<StrategyCandidate> candidates = provider.candidates(context);
                if (candidates == null) continue;
                for (StrategyCandidate candidate : candidates)
                {
                    if (candidate != null
                            && candidate.getConfidence()
                            != RecommendationConfidence.BLOCKED)
                    {
                        recommendations.add(candidate.toRecommendation());
                    }
                }
            }
        }

        recommendations.sort(
                Comparator.comparingDouble(Recommendation::getScore).reversed());
        if (recommendations.size() > 3)
        {
            recommendations = new ArrayList<>(recommendations.subList(0, 3));
        }

        List<Opportunity> opportunities = opportunityEngine.evaluate(data);
        List<StrategySignal> signals = new ArrayList<>();
        for (StrategyModule module : moduleRegistry.getModules())
        {
            List<StrategySignal> moduleSignals = module.analyze(context);
            if (moduleSignals != null) signals.addAll(moduleSignals);
        }
        signals.sort(Comparator.comparingDouble(
                StrategySignal::getScoreDelta).reversed());

        return new StrategyResult(recommendations, opportunities, signals);
    }
}
