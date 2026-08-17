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
    private final RecommendationActionabilityPolicy actionabilityPolicy;

    @Inject
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry,
            StrategyCandidateRegistry candidateRegistry,
            RecommendationActionabilityPolicy actionabilityPolicy)
    {
        this.recommendationEngine = recommendationEngine;
        this.opportunityEngine = opportunityEngine;
        this.moduleRegistry = moduleRegistry;
        this.candidateRegistry = candidateRegistry;
        this.actionabilityPolicy = actionabilityPolicy == null
                ? new RecommendationActionabilityPolicy()
                : actionabilityPolicy;
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry,
            StrategyCandidateRegistry candidateRegistry)
    {
        this(recommendationEngine, opportunityEngine, moduleRegistry,
                candidateRegistry, new RecommendationActionabilityPolicy());
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry)
    {
        this(recommendationEngine, opportunityEngine, moduleRegistry, null,
                new RecommendationActionabilityPolicy());
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

        // The global queue needs the complete skill candidate pool. Trimming to
        // three inside RecommendationEngine can hide a lower-scoring executable
        // action behind three unresolved skills before actionability is checked.
        List<Recommendation> pool = new ArrayList<>(
                recommendationEngine.recommendAll(
                        data,
                        context.getStrategyMode(),
                        context.getSessionIntent(),
                        context.isUseGroupStorage(),
                        context.isAllowWildernessMethods(),
                        context.getPreferenceProfile()));

        if (candidateRegistry != null)
        {
            for (StrategyCandidateProvider provider : candidateRegistry.getProviders())
            {
                List<StrategyCandidate> candidates = provider.candidates(context);
                if (candidates == null) continue;
                for (StrategyCandidate candidate : candidates)
                {
                    if (candidate == null
                            || candidate.getConfidence() == RecommendationConfidence.BLOCKED)
                    {
                        continue;
                    }
                    pool.add(candidate.toRecommendation());
                }
            }
        }

        List<Recommendation> recommendations = buildPlayerQueue(pool);
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

    /**
     * A high raw score cannot buy its way into DO NEXT while the candidate is
     * still unresolved. Ready actions are ranked against ready actions first;
     * Check Needed work is allowed only in the secondary slots and only when a
     * real primary action exists.
     */
    List<Recommendation> buildPlayerQueue(List<Recommendation> pool)
    {
        if (pool == null || pool.isEmpty()) return Collections.emptyList();

        List<Recommendation> ready = new ArrayList<>();
        List<Recommendation> secondary = new ArrayList<>();
        for (Recommendation recommendation : pool)
        {
            if (!actionabilityPolicy.mayAppearAsAlternative(recommendation)) continue;
            if (actionabilityPolicy.canLeadQueue(recommendation)) ready.add(recommendation);
            else secondary.add(recommendation);
        }

        Comparator<Recommendation> byScore = Comparator
                .comparingDouble(Recommendation::getScore)
                .reversed();
        ready.sort(byScore);
        secondary.sort(byScore);

        // Never put a Needs Info recommendation in the primary slot merely to
        // avoid an empty card. No recommendation is safer than false certainty.
        if (ready.isEmpty()) return Collections.emptyList();

        List<Recommendation> result = new ArrayList<>(3);
        for (Recommendation recommendation : ready)
        {
            if (result.size() >= 3) break;
            result.add(recommendation);
        }
        for (Recommendation recommendation : secondary)
        {
            if (result.size() >= 3) break;
            result.add(recommendation);
        }
        return result;
    }
}
