package compass;

import java.util.List;

/** Test-only compatibility wiring for production constructors intentionally kept lean. */
final class TestFixtures
{
    private TestFixtures() { }

    static AccountResourcePlanner accountResourcePlanner()
    {
        return new AccountResourcePlanner(
                null, new MainEconomyPlanner(), new ResourceSourceCatalog());
    }

    static FarmingRunPlanner farmingRunPlanner(FarmingRunCatalog catalog)
    {
        return new FarmingRunPlanner(catalog,
                new FarmingSupplyCatalog(), new ResourceReadinessService());
    }

    static AdaptiveMilestoneGuidanceService adaptiveMilestoneGuidanceService(
            RuneLiteSkillActionCatalog actions,
            MethodExecutionProfileCatalog profiles)
    {
        return adaptiveMilestoneGuidanceService(actions, profiles,
                new SkillingXpModifierService());
    }

    static AdaptiveMilestoneGuidanceService adaptiveMilestoneGuidanceService(
            RuneLiteSkillActionCatalog actions,
            MethodExecutionProfileCatalog profiles,
            SkillingXpModifierService modifiers)
    {
        return new AdaptiveMilestoneGuidanceService(
                actions, profiles, modifiers,
                new AdaptiveActionSelector(), new MethodInputResolver(),
                accountResourcePlanner());
    }

    static RecommendationGuidanceService recommendationGuidanceService(
            AdaptiveMilestoneGuidanceService adaptive)
    {
        return new RecommendationGuidanceService(adaptive, null, null);
    }

    static RecommendationGuidanceService recommendationGuidanceService()
    {
        RuneLiteSkillActionCatalog actions = new RuneLiteSkillActionCatalog();
        SkillingXpModifierService modifiers = new SkillingXpModifierService();
        return new RecommendationGuidanceService(
                adaptiveMilestoneGuidanceService(actions,
                        new MethodExecutionProfileCatalog(), modifiers),
                new VariableMethodGuidanceService(),
                new UniversalSkillActionGuidanceService(
                        actions, new UniversalActionRecipeResolver(), modifiers,
                        accountResourcePlanner()));
    }

    static RecommendationEngine recommendationEngine(
            TrainingMethodSelector selector)
    {
        return new RecommendationEngine(selector,
                recommendationGuidanceService(),
                new CombatGuidanceService(), new SlayerGuidanceService(),
                new SailingGuidanceService(), new SkillBreakpointService(),
                new AdaptiveActionSelector());
    }

    static RecommendationEngine recommendationEngine(
            TrainingMethodSelector selector,
            RecommendationGuidanceService guidance)
    {
        return new RecommendationEngine(selector, guidance,
                new CombatGuidanceService(), new SlayerGuidanceService(),
                new SailingGuidanceService(), new SkillBreakpointService(),
                new AdaptiveActionSelector());
    }

    private static QuestRecommendationValueService questValue()
    {
        return new QuestRecommendationValueService(questPathPlanningService());
    }

    static StrategyEngine strategyEngine(
            RecommendationEngine recommendations,
            OpportunityEngine opportunities,
            Object unused,
            StrategyCandidateRegistry registry,
            ActionabilityPolicy actionability)
    {
        return new StrategyEngine(recommendations, opportunities, registry,
                actionability, null, null, null, null, questValue(), null);
    }

    static StrategyEngine strategyEngine(
            RecommendationEngine recommendations,
            OpportunityEngine opportunities,
            Object unused,
            StrategyCandidateRegistry registry,
            ActionabilityPolicy actionability,
            RecommendationIntelligenceService intelligence)
    {
        return new StrategyEngine(recommendations, opportunities, registry,
                actionability, intelligence, null, null, null, questValue(), null);
    }

    static StrategyEngine strategyEngine(
            RecommendationEngine recommendations,
            OpportunityEngine opportunities,
            Object unused,
            StrategyCandidateRegistry registry,
            ActionabilityPolicy actionability,
            RecommendationIntelligenceService intelligence,
            CandidateSafetyPolicy safety,
            GoalDependencyProvenanceService provenance)
    {
        return new StrategyEngine(recommendations, opportunities, registry,
                actionability, intelligence, safety, provenance,
                null, questValue(), null);
    }

    static QuestRequirementResolver questRequirementResolver()
    {
        return new QuestRequirementResolver(null, null);
    }

    static QuestCandidateProvider questCandidateProvider(
            QuestPriorityCatalog priorities)
    {
        return new QuestCandidateProvider(priorities,
                new QuestKnowledgeCatalog(), questRequirementResolver(),
                new GoalDependencyProvenanceService());
    }

    static QuestCandidateProvider questCandidateProvider(
            QuestPriorityCatalog priorities,
            QuestKnowledgeCatalog knowledge,
            QuestRequirementResolver resolver)
    {
        return new QuestCandidateProvider(priorities, knowledge, resolver,
                new GoalDependencyProvenanceService());
    }

    static QuestPathPlanningService questPathPlanningService()
    {
        return new QuestPathPlanningService(new GoalGraph(),
                new QuestKnowledgeCatalog(), questRequirementResolver());
    }

    static MilestoneTracker milestoneTracker()
    {
        return new MilestoneTracker(new ProgressionObjectiveService(
                new ProgressionObjectiveCatalog()));
    }

    static LiveClueStateReader liveClueStateReader()
    {
        return new LiveClueStateReader(null, null, null);
    }

    static StrategyDataAssembler strategyDataAssembler(
            AccountReader accounts,
            LiveItemStateReader items,
            LiveQuestStateReader quests,
            AccountAccessMemoryStore access,
            FarmingRunStateStore farmingRuns,
            FarmingAccessEvaluator farming,
            ObservedStateStore observed)
    {
        return new StrategyDataAssembler(accounts, items, null, quests,
                null, null, null, null, null, null, null, null, null,
                access, farmingRuns, farming, observed);
    }

    static SlayerSnapshot slayerSnapshot(
            String task, int remaining, String master, int points,
            Confidence confidence)
    {
        return slayerSnapshot(task, remaining, master, null, points,
                null, null, null, null, null, null, null, confidence);
    }

    static SlayerSnapshot slayerSnapshot(
            String task, int remaining, String master, String location,
            int points, Confidence confidence)
    {
        return slayerSnapshot(task, remaining, master, location, points,
                null, null, null, null, null, null, null, confidence);
    }

    static SlayerSnapshot slayerSnapshot(
            String task, int remaining, String master, String location,
            int points, Integer streak, Integer questPoints,
            Integer capacity, Integer occupied, Confidence confidence)
    {
        return slayerSnapshot(task, remaining, master, location, points,
                streak, questPoints, capacity, occupied,
                null, null, null, confidence);
    }

    static SlayerSnapshot slayerSnapshot(
            String task, int remaining, String master, String location,
            int points, Integer streak, Integer questPoints,
            Integer capacity, Integer occupied,
            SlayerRewardSnapshot rewards, Confidence confidence)
    {
        return slayerSnapshot(task, remaining, master, location, points,
                streak, questPoints, capacity, occupied,
                rewards, null, null, confidence);
    }

    static SlayerSnapshot slayerSnapshot(
            String task, int remaining, String master, String location,
            int points, Integer streak, Integer questPoints,
            Integer capacity, Integer occupied,
            SlayerRewardSnapshot rewards, List<SlayerTaskOffer> offers,
            Boolean mortimerIntroduced, Confidence confidence)
    {
        return new SlayerSnapshot(task, remaining, master, location, points,
                streak, questPoints, capacity, occupied,
                rewards, offers, mortimerIntroduced, confidence);
    }
}
