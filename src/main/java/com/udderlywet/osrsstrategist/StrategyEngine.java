package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Top-level strategist coordinator. */
@Singleton
public class StrategyEngine
{
    private final RecommendationEngine recommendationEngine;
    private final OpportunityEngine opportunityEngine;
    private final StrategyCandidateRegistry candidateRegistry;
    private final ActionabilityPolicy actionabilityPolicy;
    private final RecommendationIntelligenceService intelligenceService;
    private final CandidateSafetyPolicy candidateSafetyPolicy;
    private final GoalDependencyProvenanceService goalProvenanceService;
    private final RecommendationDeduplicator deduplicator =
            new RecommendationDeduplicator();
    private final StrategicPlanService strategicPlanService =
            new StrategicPlanService();
    private final InfrastructureRecommendationValueService infrastructureValue =
            new InfrastructureRecommendationValueService();
    private final MethodRecommendationValueService methodValue;
    private final FinalExecutionPlanValidator finalExecutionValidator;
    private final ActivityStrategyKnowledgeService activityStrategyKnowledge =
            new ActivityStrategyKnowledgeService();
    private final QuestRecommendationValueService questValue =
            new QuestRecommendationValueService();
    private static final FarmingAccessCatalog FARMING_ACCESS_CATALOG =
            new FarmingAccessCatalog();

    @Inject
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyCandidateRegistry candidateRegistry,
            ActionabilityPolicy actionabilityPolicy,
            RecommendationIntelligenceService intelligenceService,
            CandidateSafetyPolicy candidateSafetyPolicy,
            GoalDependencyProvenanceService goalProvenanceService,
            MethodRecommendationValueService methodValue,
            FinalExecutionPlanValidator finalExecutionValidator)
    {
        this.recommendationEngine = recommendationEngine;
        this.opportunityEngine = opportunityEngine;
        this.candidateRegistry = candidateRegistry;
        this.actionabilityPolicy = actionabilityPolicy == null
                ? new ActionabilityPolicy()
                : actionabilityPolicy;
        this.intelligenceService = intelligenceService == null
                ? new RecommendationIntelligenceService()
                : intelligenceService;
        this.candidateSafetyPolicy = candidateSafetyPolicy == null
                ? new CandidateSafetyPolicy() : candidateSafetyPolicy;
        this.goalProvenanceService = goalProvenanceService == null
                ? new GoalDependencyProvenanceService() : goalProvenanceService;
        this.methodValue = methodValue == null
                ? new MethodRecommendationValueService() : methodValue;
        this.finalExecutionValidator = finalExecutionValidator == null
                ? new FinalExecutionPlanValidator() : finalExecutionValidator;
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            Object unusedModules,
            StrategyCandidateRegistry candidateRegistry,
            ActionabilityPolicy actionabilityPolicy,
            RecommendationIntelligenceService intelligenceService,
            CandidateSafetyPolicy candidateSafetyPolicy,
            GoalDependencyProvenanceService goalProvenanceService)
    {
        this(recommendationEngine, opportunityEngine,
                candidateRegistry, actionabilityPolicy, intelligenceService,
                candidateSafetyPolicy, goalProvenanceService,
                new MethodRecommendationValueService(),
                new FinalExecutionPlanValidator());
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            Object unusedModules,
            StrategyCandidateRegistry candidateRegistry,
            ActionabilityPolicy actionabilityPolicy,
            RecommendationIntelligenceService intelligenceService)
    {
        this(recommendationEngine, opportunityEngine,
                candidateRegistry, actionabilityPolicy, intelligenceService,
                new CandidateSafetyPolicy(), new GoalDependencyProvenanceService(),
                new MethodRecommendationValueService(),
                new FinalExecutionPlanValidator());
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            Object unusedModules,
            StrategyCandidateRegistry candidateRegistry,
            ActionabilityPolicy actionabilityPolicy)
    {
        this(recommendationEngine, opportunityEngine,
                candidateRegistry, actionabilityPolicy,
                new RecommendationIntelligenceService(),
                new CandidateSafetyPolicy(), new GoalDependencyProvenanceService(),
                new MethodRecommendationValueService(),
                new FinalExecutionPlanValidator());
    }

    public StrategyResult evaluate(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        return evaluate(data, strategyMode, sessionIntent,
                QuestTolerance.NORMAL, GoalType.MAX,
                true, false, false, preferenceProfile);
    }

    public StrategyResult evaluate(
            GameData data,
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
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile)
    {
        if (data == null || data.account() == null)
        {
            return new StrategyResult(
                    Collections.singletonList(
                            FallbackRecommendationFactory.forState(data)),
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
                    || opportunity.getConfidence() == Confidence.BLOCKED
                    || context.preferenceProfile().isOnCooldown(
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
                        context.preferenceProfile()));

        if (candidateRegistry != null)
        {
            for (CandidateProvider provider : candidateRegistry.getProviders())
            {
                var candidates = provider.candidates(context);
                if (candidates == null || candidates.isEmpty()) continue;
                var superseded = provider.supersededCandidateIds();
                if (superseded != null && !superseded.isEmpty())
                    pool.removeIf(value -> value != null
                            && superseded.contains(value.getId()));
                for (Recommendation candidate : candidates)
                {
                    if (candidate == null
                            || candidate.getConfidence() == Confidence.BLOCKED)
                    {
                        continue;
                    }
                    Recommendation sourced = activityStrategyKnowledge.attach(
                            candidate, context);
                    if (sourced != null) pool.add(sourced);
                }
            }
        }

        for (Opportunity opportunity : opportunities)
        {
            var promoted = opportunityRecommendation(opportunity, context);
            if (promoted != null) pool.add(promoted);
        }

        List<Recommendation> attributed = new ArrayList<>(pool.size());
        for (Recommendation recommendation : pool)
            attributed.add(finalExecutionValidator.validate(methodValue.attach(questValue.attach(
                    infrastructureValue.attach(
                            goalProvenanceService.attach(recommendation,
                                    context), context), context), context), context));
        pool = attributed;

        // Only after legality/actionability is known do we compare account value
        // across skills, quests, upgrades, detours, PvM, gear and minigames.
        var recommendations = buildPlayerQueue(pool, context);
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
        StrategicPlan plan = strategicPlanService.build(
                recommendations, context, System.currentTimeMillis());
        return new StrategyResult(recommendations, opportunities, plan);
    }

    Recommendation opportunityRecommendation(
            Opportunity opportunity,
            StrategyContext context)
    {
        if (opportunity == null || !opportunity.isReady()
                || opportunity.getConfidence() != Confidence.VERIFIED)
        {
            return null;
        }
        var id = opportunity.getId();
        var preferences = context.preferenceProfile();
        if (preferences.isOnCooldown(id)) return null;

        var setupVerified = opportunity.isSetupVerified();
        if (!setupVerified && opportunity.getPreparation().isEmpty()) return null;
        var location = opportunityLocation(opportunity.getType(), context);
        String action = opportunityAction(opportunity.getType(),
                opportunity.getTitle());
        if (location == null || action == null) return null;
        String supplies = setupVerified
                ? get(720)
                : get(1490) + String.join(", ", opportunity.getPreparation()) + ".";
        double score = 46.0 + preferences.weightFor(id) * 10.0
                + preferences.timedScoreAdjustmentFor(id);
        if (opportunity.getType() == OpportunityType.HERB_RUN
                || opportunity.getType() == OpportunityType.BIRDHOUSE_RUN
                || opportunity.getType() == OpportunityType.FARMING_CONTRACT)
        {
            score += 8.0;
        }
        Guidance guidance = new Guidance(
                setupVerified
                        ? action
                        : get(722) + opportunity.getTitle()
                                + get(1491),
                supplies,
                location,
                setupVerified
                        ? get(723)
                        : get(724));
        return new Recommendation(
                id, opportunity.getTitle(),
                get(725),
                score, null, setupVerified
                        ? Confidence.VERIFIED
                        : Confidence.CHECK_NEEDED,
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
                return get(726);
            case HERB_RUN:
                return get(727);
            case BATTLESTAVES:
                return get(728);
            default:
                return null;
        }
    }

    private static String opportunityLocation(
            OpportunityType type, StrategyContext context)
    {
        if (type == null) return null;
        switch (type)
        {
            case BIRDHOUSE_RUN:
                return get(729);
            case HERB_RUN:
                return verifiedHerbPatchRoute(context);
            case BATTLESTAVES:
                return get(721);
            default:
                return null;
        }
    }

    private static String verifiedHerbPatchRoute(StrategyContext context)
    {
        FarmingSnapshot farming = context == null || context.data() == null
                ? null : context.data().farming();
        if (farming == null) return null;
        List<String> names = new ArrayList<>();
        for (FarmingAccessDefinition definition : FARMING_ACCESS_CATALOG.all())
        {
            if (definition.isHerbPatch()
                    && farming.isPatchReachable(definition.getId()))
            {
                names.add(definition.getDisplayName());
            }
        }
        if (names.isEmpty()) return null;
        return String.join(" -> ", names) + ".";
    }

    private static SafetyEvidence opportunitySafety(Opportunity opportunity)
    {
        switch (opportunity.getType())
        {
            case BIRDHOUSE_RUN:
                return SafetyEvidence.skill(false, net.runelite.api.Skill.HUNTER);
            case HERB_RUN:
            case TREE_RUN:
            case FARMING_CONTRACT:
                return SafetyEvidence.skill(false, net.runelite.api.Skill.FARMING);
            case KINGDOM:
            case KINGDOM_APPROVAL:
            case BATTLESTAVES:
            case DYNAMITE:
                return SafetyEvidence.harmless(false);
            case TEARS_OF_GUTHIX:
            case DAILY_DIARY_REWARD:
                return SafetyEvidence.potentiallyIrreversible(false);
            case CLUE:
                return opportunity.getSafetyEvidence();
            default:
                return SafetyEvidence.potentiallyIrreversible(false);
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
            var semanticKey = deduplicator.semanticKey(recommendation);
            if (context != null && (context.preferenceProfile()
                    .isOnCooldown(recommendation.getId())
                    || context.preferenceProfile()
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
            var dimension = alternativeDimension(recommendation);
            if (!result.isEmpty() && representedDimensions.contains(dimension))
                continue;
            result.add(recommendation);
            representedDimensions.add(dimension);
        }
    }

    static String alternativeDimension(Recommendation recommendation)
    {
        if (recommendation == null) return "unknown";
        var plan = recommendation.getTrainingPlan();
        if (plan != null && plan.getMethod() != null
                && plan.getMethod().getSkill() != null)
            return "skill:" + plan.getMethod().getSkill().name();
        String id = recommendation.getId() == null ? ""
                : recommendation.getId().toLowerCase(Locale.ROOT);
        var colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(0, colon);
    }

    private double semanticPreferenceScore(Recommendation recommendation,
            StrategyContext context)
    {
        if (recommendation == null || context == null) return 0.0;
        var key = deduplicator.semanticKey(recommendation);
        return context.preferenceProfile().semanticWeightFor(key) * 10.0
                + context.preferenceProfile()
                .semanticTimedScoreAdjustmentFor(key);
    }
}
