package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Top-level strategist coordinator.
 *
 * <p>Candidate providers answer "what is possible/useful?". The recommendation
 * engine supplies skill work, domain providers supply quests/PvM/gear/etc.,
 * actionability then adds a small preference for things the account can prove it
 * can start, and the healthy-engagement policy is the final weak adjustment
 * before ranking. This ordering keeps game truth and strategic value more
 * important than variety.</p>
 */
@Singleton
public class StrategyEngine
{
    private final RecommendationEngine recommendationEngine;
    private final OpportunityEngine opportunityEngine;
    private final StrategyModuleRegistry moduleRegistry;
    private final StrategyCandidateRegistry candidateRegistry;
    private final ActionabilityScoringPolicy actionabilityScoringPolicy;
    private final HealthyEngagementPolicy healthyEngagementPolicy;

    @Inject
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry,
            StrategyCandidateRegistry candidateRegistry,
            ActionabilityScoringPolicy actionabilityScoringPolicy,
            HealthyEngagementPolicy healthyEngagementPolicy)
    {
        this.recommendationEngine = recommendationEngine;
        this.opportunityEngine = opportunityEngine;
        this.moduleRegistry = moduleRegistry;
        this.candidateRegistry = candidateRegistry;
        this.actionabilityScoringPolicy = actionabilityScoringPolicy == null
                ? new ActionabilityScoringPolicy()
                : actionabilityScoringPolicy;
        this.healthyEngagementPolicy = healthyEngagementPolicy == null
                ? new HealthyEngagementPolicy()
                : healthyEngagementPolicy;
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry,
            StrategyCandidateRegistry candidateRegistry,
            HealthyEngagementPolicy healthyEngagementPolicy)
    {
        this(recommendationEngine, opportunityEngine, moduleRegistry,
                candidateRegistry, new ActionabilityScoringPolicy(),
                healthyEngagementPolicy);
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry,
            StrategyCandidateRegistry candidateRegistry)
    {
        this(recommendationEngine, opportunityEngine, moduleRegistry,
                candidateRegistry, new ActionabilityScoringPolicy(),
                new HealthyEngagementPolicy());
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry)
    {
        this(recommendationEngine, opportunityEngine, moduleRegistry,
                null, new ActionabilityScoringPolicy(),
                new HealthyEngagementPolicy());
    }

    public StrategyResult evaluate(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        return evaluate(data, strategyMode, sessionIntent,
                QuestTolerance.NORMAL, GoalType.MAX,
                true, false, false, preferenceProfile,
                null, VarietyPreference.BALANCED);
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
                preferenceProfile, null, VarietyPreference.BALANCED);
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
        return evaluate(data, strategyMode, sessionIntent, questTolerance,
                activeGoal, useGroupStorage, collectionistMode,
                allowWildernessMethods, preferenceProfile,
                null, VarietyPreference.BALANCED);
    }

    /**
     * Full adaptive evaluation used by the live plugin.
     *
     * @param history per-character interaction/completion history; may be null
     * @param varietyPreference how strongly near-tie recommendations may rotate
     */
    public StrategyResult evaluate(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile,
            RecommendationHistory history,
            VarietyPreference varietyPreference)
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

        // Skill training is one candidate source, not the entire product.
        List<Recommendation> recommendations = new ArrayList<>(
                recommendationEngine.recommend(
                        data,
                        context.getStrategyMode(),
                        context.getSessionIntent(),
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

        // Actionability is bounded. It lets a similarly valuable Ready option
        // beat a route with several unresolved checks, but cannot compensate for
        // a large strategic-score deficit.
        recommendations = actionabilityScoringPolicy.adjust(recommendations);

        // Healthy engagement is purposefully applied after normal strategic and
        // readiness scoring and before sorting. It is also capped, so it can
        // resolve close choices without manufacturing engagement pressure.
        recommendations = healthyEngagementPolicy.adjust(
                recommendations, history, varietyPreference);

        recommendations.sort(
                Comparator.comparingDouble(Recommendation::getScore).reversed());
        if (recommendations.size() > 3)
        {
            recommendations = new ArrayList<>(recommendations.subList(0, 3));
        }

        List<Opportunity> opportunities = opportunityEngine.evaluate(data);
        List<StrategySignal> signals = new ArrayList<>();
        if (moduleRegistry != null)
        {
            for (StrategyModule module : moduleRegistry.getModules())
            {
                List<StrategySignal> moduleSignals = module.analyze(context);
                if (moduleSignals != null) signals.addAll(moduleSignals);
            }
        }
        signals.sort(Comparator.comparingDouble(
                StrategySignal::getScoreDelta).reversed());

        return new StrategyResult(recommendations, opportunities, signals);
    }
}
