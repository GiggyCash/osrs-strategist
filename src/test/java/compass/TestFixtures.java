package compass;

import java.util.List;

/** Test-only compatibility wiring for production constructors intentionally kept lean. */
final class TestFixtures
{
    private TestFixtures() { }

    static TrainingPlan select(TrainingMethodSelector selector, GameData data,
            net.runelite.api.Skill skill, int level, StrategyMode mode,
            SessionIntent intent)
    {
        return select(selector, data, skill, level, mode, intent, false);
    }

    static TrainingPlan select(TrainingMethodSelector selector, GameData data,
            net.runelite.api.Skill skill, int level, StrategyMode mode,
            SessionIntent intent, boolean wilderness)
    {
        List<TrainingPlan> ranked = selector.rankedCandidates(data, skill, level,
                mode, intent, wilderness, false);
        return ranked.isEmpty() ? null : ranked.get(0);
    }

    static AccountResourcePlanner accountResourcePlanner()
    {
        return new AccountResourcePlanner(
                null, new ResourceSourceCatalog());
    }

    static FarmingRunPlanner farmingRunPlanner(FarmingRunCatalog catalog)
    {
        return new FarmingRunPlanner(catalog, new FarmingSupplyCatalog());
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
                new AdaptiveActionSelector(), new UniversalActionRecipeResolver(),
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
                new CombatGuidanceService(), new SailingGuidanceService(),
                new SkillBreakpointService(),
                new AdaptiveActionSelector(), new SlayerStrategist());
    }

    static RecommendationEngine recommendationEngine(
            TrainingMethodSelector selector,
            RecommendationGuidanceService guidance)
    {
        return new RecommendationEngine(selector, guidance,
                new CombatGuidanceService(), new SailingGuidanceService(),
                new SkillBreakpointService(),
                new AdaptiveActionSelector(), new SlayerStrategist());
    }

    static StrategyEngine strategyEngine(
            RecommendationEngine recommendations,
            OpportunityEngine opportunities,
            Object unused,
            StrategyCandidateRegistry registry,
            ActionabilityPolicy actionability)
    {
        return new StrategyEngine(recommendations, opportunities, registry,
                actionability, null, null, null, null);
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
                actionability, intelligence, null, null, null);
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
                null);
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

    static MilestoneTracker milestoneTracker()
    {
        return new MilestoneTracker(new ProgressionObjectiveCatalog());
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
        return new StrategyDataAssembler(null, accounts, items, null, quests,
                null, null, null, null, null, null, null, null, null,
                access, farmingRuns, farming, null, null, null, observed);
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
