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
    private final RecommendationIntelligenceService intelligenceService;
    private final CandidateSafetyPolicy candidateSafetyPolicy;
    private final RecommendationDeduplicator deduplicator =
            new RecommendationDeduplicator();

    @Inject
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry,
            StrategyCandidateRegistry candidateRegistry,
            RecommendationActionabilityPolicy actionabilityPolicy,
            RecommendationIntelligenceService intelligenceService,
            CandidateSafetyPolicy candidateSafetyPolicy)
    {
        this.recommendationEngine = recommendationEngine;
        this.opportunityEngine = opportunityEngine;
        this.moduleRegistry = moduleRegistry;
        this.candidateRegistry = candidateRegistry;
        this.actionabilityPolicy = actionabilityPolicy == null
                ? new RecommendationActionabilityPolicy()
                : actionabilityPolicy;
        this.intelligenceService = intelligenceService == null
                ? new RecommendationIntelligenceService()
                : intelligenceService;
        this.candidateSafetyPolicy = candidateSafetyPolicy == null
                ? new CandidateSafetyPolicy() : candidateSafetyPolicy;
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry,
            StrategyCandidateRegistry candidateRegistry,
            RecommendationActionabilityPolicy actionabilityPolicy,
            RecommendationIntelligenceService intelligenceService)
    {
        this(recommendationEngine, opportunityEngine, moduleRegistry,
                candidateRegistry, actionabilityPolicy, intelligenceService,
                new CandidateSafetyPolicy());
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry,
            StrategyCandidateRegistry candidateRegistry,
            RecommendationActionabilityPolicy actionabilityPolicy)
    {
        this(recommendationEngine, opportunityEngine, moduleRegistry,
                candidateRegistry, actionabilityPolicy,
                new RecommendationIntelligenceService(),
                new CandidateSafetyPolicy());
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry,
            StrategyCandidateRegistry candidateRegistry)
    {
        this(recommendationEngine, opportunityEngine, moduleRegistry,
                candidateRegistry, new RecommendationActionabilityPolicy(),
                new RecommendationIntelligenceService(),
                new CandidateSafetyPolicy());
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry)
    {
        this(recommendationEngine, opportunityEngine, moduleRegistry, null,
                new RecommendationActionabilityPolicy(),
                new RecommendationIntelligenceService(),
                new CandidateSafetyPolicy());
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
                    Collections.singletonList(
                            FallbackRecommendationFactory.forState(data)),
                    Collections.emptyList(),
                    Collections.emptyList());
        }

        StrategyContext context = new StrategyContext(
                data, strategyMode, sessionIntent, questTolerance, activeGoal,
                useGroupStorage, collectionistMode, allowWildernessMethods,
                preferenceProfile);

        List<Opportunity> opportunities = new ArrayList<>();
        List<Opportunity> evaluatedOpportunities = opportunityEngine == null
                ? Collections.emptyList() : opportunityEngine.evaluate(data);
        for (Opportunity opportunity : evaluatedOpportunities)
        {
            if (opportunity == null
                    || opportunity.getConfidence() == RecommendationConfidence.BLOCKED
                    || context.getPreferenceProfile().isOnCooldown(
                    opportunity.getId())
                    || !candidateSafetyPolicy.isAllowed(
                    opportunitySafety(opportunity), context)) continue;
            opportunities.add(opportunity);
        }

        // The global queue needs the complete skill candidate pool. Trimming to
        // three inside RecommendationEngine can hide a lower-scoring executable
        // action behind three unresolved skills before actionability is checked.
        List<Recommendation> pool = new ArrayList<>(
                recommendationEngine == null ? Collections.emptyList()
                : recommendationEngine.recommendAll(
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

        for (Opportunity opportunity : opportunities)
        {
            Recommendation promoted = opportunityRecommendation(opportunity, context);
            if (promoted != null) pool.add(promoted);
        }

        // Only after legality/actionability is known do we compare account value
        // across skills, quests, upgrades, detours, PvM, gear and minigames.
        List<Recommendation> recommendations = buildPlayerQueue(pool, context);
        if (recommendations.isEmpty())
        {
            Recommendation preparation = highestValuePreparation(pool, context);
            recommendations = Collections.singletonList(preparation == null
                    ? FallbackRecommendationFactory.forState(data)
                    : preparation);
        }
        if (!recommendations.isEmpty())
        {
            java.util.Set<String> promotedIds = new java.util.HashSet<>();
            for (Recommendation recommendation : recommendations)
                if (recommendation.getId().startsWith("opportunity:"))
                    promotedIds.add(recommendation.getId());
            opportunities.removeIf(value -> promotedIds.contains(value.getId()));
        }
        List<StrategySignal> signals = new ArrayList<>();
        for (StrategyModule module : moduleRegistry == null
                ? Collections.<StrategyModule>emptyList() : moduleRegistry.getModules())
        {
            List<StrategySignal> moduleSignals = module.analyze(context);
            if (moduleSignals != null) signals.addAll(moduleSignals);
        }
        signals.sort(Comparator.comparingDouble(
                StrategySignal::getScoreDelta).reversed());

        return new StrategyResult(recommendations, opportunities, signals);
    }

    private Recommendation highestValuePreparation(List<Recommendation> pool,
            StrategyContext context)
    {
        if (pool == null) return null;
        Recommendation best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Recommendation candidate : pool)
        {
            if (candidate == null
                    || candidate.getConfidence() != RecommendationConfidence.CHECK_NEEDED
                    || !candidateSafetyPolicy.isAllowed(candidate, context)
                    || !actionabilityPolicy.mayAppearAsAlternative(candidate)) continue;
            RecommendationGuidance guidance = candidate.getGuidance();
            if (guidance == null || guidance.getAction() == null
                    || guidance.getAction().trim().isEmpty()) continue;
            double score = intelligenceService.rankScore(candidate, context);
            if (best == null || score > bestScore)
            {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    Recommendation opportunityRecommendation(
            Opportunity opportunity,
            StrategyContext context)
    {
        if (opportunity == null || !opportunity.isReady()
                || opportunity.getConfidence() != RecommendationConfidence.VERIFIED)
        {
            return null;
        }
        String id = opportunity.getId();
        PreferenceProfile preferences = context.getPreferenceProfile();
        if (preferences.isOnCooldown(id)) return null;

        boolean setupVerified = opportunity.isSetupVerified();
        if (!setupVerified && opportunity.getPreparation().isEmpty()) return null;
        String supplies = setupVerified
                ? "No additional preparation is recorded for this observed ready opportunity."
                : "Before leaving, verify: " + String.join(", ", opportunity.getPreparation()) + ".";
        double score = 46.0 + preferences.weightFor(id) * 10.0
                + preferences.timedScoreAdjustmentFor(id);
        if (opportunity.getType() == OpportunityType.HERB_RUN
                || opportunity.getType() == OpportunityType.BIRDHOUSE_RUN
                || opportunity.getType() == OpportunityType.FARMING_CONTRACT)
        {
            score += 8.0;
        }
        RecommendationGuidance guidance = new RecommendationGuidance(
                setupVerified
                        ? "Complete the observed ready " + opportunity.getTitle() + " now."
                        : "Verify the listed setup for the ready " + opportunity.getTitle()
                                + " before starting it.",
                supplies,
                "Use the verified route associated with this observed opportunity.",
                setupVerified
                        ? "The ready state was observed and no remaining setup checks are known."
                        : "The timer is ready, but this remains a preparation alternative until the listed setup is verified.");
        return new Recommendation(
                id, opportunity.getTitle(),
                "This observed ready opportunity is time-sensitive and can compete with ordinary progression.",
                score, null, setupVerified
                        ? RecommendationConfidence.VERIFIED
                        : RecommendationConfidence.CHECK_NEEDED,
                0, 0, guidance,
                opportunitySafety(opportunity));
    }

    private static CandidateSafetyEvidence opportunitySafety(Opportunity opportunity)
    {
        switch (opportunity.getType())
        {
            case BIRDHOUSE_RUN:
                return CandidateSafetyEvidence.skill(false, net.runelite.api.Skill.HUNTER);
            case HERB_RUN:
            case TREE_RUN:
            case FARMING_CONTRACT:
                return CandidateSafetyEvidence.skill(false, net.runelite.api.Skill.FARMING);
            case KINGDOM:
            case KINGDOM_APPROVAL:
            case BATTLESTAVES:
            case DYNAMITE:
                return CandidateSafetyEvidence.harmless(false);
            case TEARS_OF_GUTHIX:
            case DAILY_DIARY_REWARD:
                return CandidateSafetyEvidence.potentiallyIrreversible(false);
            case CLUE:
                return opportunity.getSafetyEvidence();
            default:
                return CandidateSafetyEvidence.potentiallyIrreversible(false);
        }
    }

    /** Compatibility entry used by focused queue/actionability tests. */
    List<Recommendation> buildPlayerQueue(List<Recommendation> pool)
    {
        return buildPlayerQueue(pool, null);
    }

    /**
     * A high raw score cannot buy its way into DO NEXT while the candidate is
     * still unresolved. Ready actions are ranked against ready actions first;
     * Check Needed work is allowed only in the secondary slots and only when a
     * real primary action exists.
     */
    List<Recommendation> buildPlayerQueue(
            List<Recommendation> pool,
            StrategyContext context)
    {
        if (pool == null || pool.isEmpty()) return Collections.emptyList();

        List<Recommendation> ready = new ArrayList<>();
        List<Recommendation> secondary = new ArrayList<>();
        for (Recommendation recommendation : deduplicator.deduplicate(pool))
        {
            String semanticKey = deduplicator.semanticKey(recommendation);
            if (context != null && (context.getPreferenceProfile()
                    .isOnCooldown(recommendation.getId())
                    || context.getPreferenceProfile()
                    .isSemanticOnCooldown(semanticKey))) continue;
            if (!candidateSafetyPolicy.isAllowed(recommendation, context)) continue;
            if (!actionabilityPolicy.mayAppearAsAlternative(recommendation)) continue;
            if (actionabilityPolicy.canLeadQueue(recommendation)) ready.add(recommendation);
            else secondary.add(recommendation);
        }

        Comparator<Recommendation> byAccountValue = Comparator
                .comparingDouble((Recommendation recommendation) ->
                        intelligenceService.rankScore(recommendation, context)
                                + semanticPreferenceScore(recommendation,
                                        context))
                .reversed()
                .thenComparing(Recommendation::getId,
                        Comparator.nullsLast(String::compareTo));
        ready.sort(byAccountValue);
        secondary.sort(byAccountValue);

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

    private double semanticPreferenceScore(Recommendation recommendation,
            StrategyContext context)
    {
        if (recommendation == null || context == null) return 0.0;
        String key = deduplicator.semanticKey(recommendation);
        return context.getPreferenceProfile().semanticWeightFor(key) * 10.0
                + context.getPreferenceProfile()
                .semanticTimedScoreAdjustmentFor(key);
    }
}
