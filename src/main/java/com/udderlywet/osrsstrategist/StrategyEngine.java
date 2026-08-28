package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    private final GoalDependencyProvenanceService goalProvenanceService;
    private final RecommendationDeduplicator deduplicator =
            new RecommendationDeduplicator();
    private final StrategicPlanService strategicPlanService =
            new StrategicPlanService();
    private final InfrastructureRecommendationValueService infrastructureValue =
            new InfrastructureRecommendationValueService();
    private final MethodRecommendationValueService methodValue =
            new MethodRecommendationValueService();
    private final QuestRecommendationValueService questValue =
            new QuestRecommendationValueService();

    @Inject
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry,
            StrategyCandidateRegistry candidateRegistry,
            RecommendationActionabilityPolicy actionabilityPolicy,
            RecommendationIntelligenceService intelligenceService,
            CandidateSafetyPolicy candidateSafetyPolicy,
            GoalDependencyProvenanceService goalProvenanceService)
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
        this.goalProvenanceService = goalProvenanceService == null
                ? new GoalDependencyProvenanceService() : goalProvenanceService;
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry,
            StrategyCandidateRegistry candidateRegistry,
            RecommendationActionabilityPolicy actionabilityPolicy,
            RecommendationIntelligenceService intelligenceService,
            CandidateSafetyPolicy candidateSafetyPolicy)
    {
        this(recommendationEngine, opportunityEngine, moduleRegistry,
                candidateRegistry, actionabilityPolicy, intelligenceService,
                candidateSafetyPolicy, new GoalDependencyProvenanceService());
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
                new CandidateSafetyPolicy(), new GoalDependencyProvenanceService());
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
                new CandidateSafetyPolicy(), new GoalDependencyProvenanceService());
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
                new CandidateSafetyPolicy(), new GoalDependencyProvenanceService());
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
                new CandidateSafetyPolicy(), new GoalDependencyProvenanceService());
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
                        context.getActiveGoal(),
                        context.getPreferenceProfile()));

        if (candidateRegistry != null)
        {
            for (StrategyCandidateProvider provider : candidateRegistry.getProviders())
            {
                List<StrategyCandidate> candidates = provider.candidates(context);
                if (candidates == null || candidates.isEmpty()) continue;
                Set<String> superseded = provider.supersededCandidateIds();
                if (superseded != null && !superseded.isEmpty())
                    pool.removeIf(value -> value != null
                            && superseded.contains(value.getId()));
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

        List<Recommendation> attributed = new ArrayList<>(pool.size());
        for (Recommendation recommendation : pool)
            attributed.add(methodValue.attach(questValue.attach(
                    infrastructureValue.attach(
                            goalProvenanceService.attach(recommendation,
                                    context), context), context), context));
        pool = attributed;

        // Only after legality/actionability is known do we compare account value
        // across skills, quests, upgrades, detours, PvM, gear and minigames.
        List<Recommendation> recommendations = buildPlayerQueue(pool, context);
        if (recommendations.isEmpty())
        {
            recommendations = Collections.singletonList(
                    FallbackRecommendationFactory.forState(data));
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

        StrategicPlan plan = strategicPlanService.build(
                recommendations, context, System.currentTimeMillis());
        return new StrategyResult(recommendations, opportunities, signals,
                plan);
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
        String location = opportunityLocation(opportunity.getType());
        String action = opportunityAction(opportunity.getType(),
                opportunity.getTitle());
        if (location == null || action == null) return null;
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
                        ? action
                        : "Verify the listed setup for the ready " + opportunity.getTitle()
                                + " before starting it.",
                supplies,
                location,
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

    private static String opportunityAction(
            OpportunityType type, String title)
    {
        if (type == null) return null;
        switch (type)
        {
            case BIRDHOUSE_RUN:
                return "Empty all four birdhouses, rebuild them with the carried logs and clockworks, seed them, then leave the Fossil Island route.";
            case HERB_RUN:
                return "Harvest each verified reachable herb patch, compost it, and replant with the carried herb seeds.";
            case BATTLESTAVES:
                return "Buy the available daily battlestaves from Zaff.";
            default:
                return null;
        }
    }

    private static String opportunityLocation(OpportunityType type)
    {
        if (type == null) return null;
        switch (type)
        {
            case BIRDHOUSE_RUN:
                return "The four birdhouse spaces on Fossil Island.";
            case HERB_RUN:
                return "The reachable herb patches in the active Farming checklist.";
            case BATTLESTAVES:
                return "Zaff's Superior Staves in central Varrock.";
            default:
                return null;
        }
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
            recommendation = goalProvenanceService.attach(
                    recommendation, context);
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
        Set<String> representedDimensions = new HashSet<>();
        addDiverse(result, representedDimensions, ready);
        addDiverse(result, representedDimensions, secondary);
        return result;
    }

    /**
     * Alternative slots are product choices, not an unfiltered scoreboard.
     * Keep at most one candidate per activity dimension unless the candidates
     * are different skills, which are inherently different playable sessions.
     */
    private static void addDiverse(List<Recommendation> result,
            Set<String> representedDimensions, List<Recommendation> candidates)
    {
        for (Recommendation recommendation : candidates)
        {
            if (result.size() >= 3) return;
            String dimension = alternativeDimension(recommendation);
            if (!result.isEmpty() && representedDimensions.contains(dimension))
                continue;
            result.add(recommendation);
            representedDimensions.add(dimension);
        }
    }

    static String alternativeDimension(Recommendation recommendation)
    {
        if (recommendation == null) return "unknown";
        TrainingPlan plan = recommendation.getTrainingPlan();
        if (plan != null && plan.getMethod() != null
                && plan.getMethod().getSkill() != null)
            return "skill:" + plan.getMethod().getSkill().name();
        String id = recommendation.getId() == null ? ""
                : recommendation.getId().toLowerCase(Locale.ROOT);
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(0, colon);
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
