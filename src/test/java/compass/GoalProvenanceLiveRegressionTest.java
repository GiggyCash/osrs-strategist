package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GoalProvenanceLiveRegressionTest
{
    private final GoalDependencyProvenanceService provenance =
            new GoalDependencyProvenanceService();

    @Test
    public void farmingTenIsNotABarrowsGlovesPrerequisite()
    {
        StrategyContext context = context(GoalType.BARROWS_GLOVES,
                QuestTolerance.LOW, rfdEarlyStatuses(false));
        Recommendation farming = skillRecommendation(
                Skill.FARMING, 1, 10, 50.0, "farming_falador_potatoes");

        Recommendation attributed = provenance.attach(farming, context);
        assertNull(attributed.getGoalProvenance());
        assertEquals(GoalRelation.FALLBACK,
                GoalRecommendationContext.assess(GoalType.BARROWS_GLOVES,
                        attributed, Membership.P2P).getRelationship());
    }

    @Test
    public void barrowsModelSeparatesRequirementsAndProvesRealSkillPath()
    {
        StrategyContext context = context(GoalType.BARROWS_GLOVES,
                QuestTolerance.NORMAL, allQuestsIncomplete());
        Recommendation fishing = provenance.attach(skillRecommendation(
                Skill.FISHING, 1, 10, 30, "fishing_test"), context);
        assertTrue(fishing.getGoalProvenance().compactPath()
                .contains("53 Fishing"));
    }

    @Test
    public void relationshipsRequireAValidatedPathAndSiblingGoalsFailClosed()
    {
        StrategyContext barrows = context(GoalType.BARROWS_GLOVES,
                QuestTolerance.NORMAL, rfdEarlyStatuses(false));
        Recommendation unproven = skillRecommendation(
                Skill.SLAYER, 1, 10, 50.0, "slayer_test");
        assertEquals(0.0, RecommendationIntelligenceService.goalValue(
                unproven, GoalType.BARROWS_GLOVES), 0.0);
        assertEquals(GoalRelation.FALLBACK,
                GoalRecommendationContext.assess(GoalType.BARROWS_GLOVES,
                        unproven, Membership.P2P).getRelationship());

        for (GoalType goal : new GoalType[]{GoalType.FIRE_CAPE,
                GoalType.QUEST_CAPE, GoalType.PRIFDDINAS, GoalType.BOWFA,
                GoalType.INFERNAL_CAPE})
        {
            Recommendation attributed = provenance.attach(unproven,
                    context(goal, QuestTolerance.NORMAL,
                            rfdEarlyStatuses(false)));
            assertNull("No arbitrary skill relationship for " + goal,
                    attributed.getGoalProvenance());
        }

        Recommendation unrelatedQuest = new Recommendation(
                "quest:x-marks-the-spot", "Quest: X Marks the Spot",
                "Optional quest.", 20, Confidence.VERIFIED, null,
                Safety.unknown());
        assertNull(provenance.attach(unrelatedQuest,
                context(GoalType.MAX, QuestTolerance.NORMAL,
                        Collections.singletonMap("X Marks the Spot",
                                QuestStatus.NOT_STARTED))).getGoalProvenance());
    }

    @Test
    public void requiredGoalQuestIgnoresLowOptionalQuestPreference()
    {
        Map<String, QuestStatus> statuses = rfdEarlyStatuses(false);
        statuses.put("Below Ice Mountain", QuestStatus.NOT_STARTED);
        StrategyContext low = context(GoalType.BARROWS_GLOVES,
                QuestTolerance.LOW, statuses);
        List<Recommendation> candidates = TestFixtures.questCandidateProvider(
                new QuestPriorityCatalog()).candidates(low);
        Recommendation required = named(candidates, "Wartface & Bentnoze");
        Recommendation optional = named(candidates, "Below Ice Mountain");

        assertTrue(required.getReason().contains("proven quest path"));
        assertTrue(required.getScore() > optional.getScore());
    }

    @Test
    public void lowPreferenceStillPenalizesUnrelatedOptionalQuest()
    {
        Map<String, QuestStatus> statuses = new LinkedHashMap<>();
        statuses.put("Below Ice Mountain", QuestStatus.NOT_STARTED);
        Recommendation low = named(TestFixtures.questCandidateProvider(
                        new QuestPriorityCatalog()).candidates(context(
                                GoalType.BARROWS_GLOVES, QuestTolerance.LOW,
                                statuses)), "Below Ice Mountain");
        Recommendation high = named(TestFixtures.questCandidateProvider(
                        new QuestPriorityCatalog()).candidates(context(
                                GoalType.BARROWS_GLOVES, QuestTolerance.HIGH,
                                statuses)), "Below Ice Mountain");
        assertTrue(high.getScore() > low.getScore());
    }

    @Test
    public void guaranteedGoalQuestXpPricesRedundantManualTraining()
    {
        Recommendation farming = skillRecommendation(
                Skill.FARMING, 1, 10, 50.0, "farming_falador_potatoes");
        StrategyContext pending = context(GoalType.BARROWS_GLOVES,
                QuestTolerance.LOW, rfdEarlyStatuses(false));
        StrategyContext complete = context(GoalType.BARROWS_GLOVES,
                QuestTolerance.LOW, rfdEarlyStatuses(true));

        GoalQuestRewardForecast forecast = provenance
                .guaranteedRewardsBeforeManualTraining(pending, Skill.FARMING);
        assertEquals(1_000, forecast.getExperience());
        assertTrue(forecast.getSourceQuests().stream()
                .anyMatch(name -> name.contains("Wartface")));

        RecommendationIntelligenceService intelligence =
                new RecommendationIntelligenceService();
        assertTrue(intelligence.rankScore(farming, pending)
                < intelligence.rankScore(farming, complete));
    }

    @Test
    public void unprovenGoalDoesNotRenderAnEmptyGoalSection()
    {
        Recommendation farming = skillRecommendation(
                Skill.FARMING, 1, 10, 50.0, "farming_falador_potatoes");
        GoalRecommendationContext goal = GoalRecommendationContext.assess(
                GoalType.BARROWS_GLOVES, farming, Membership.P2P);
        assertFalse(Presentation.compactText(farming, goal)
                .contains("GOAL"));
        assertFalse(Presentation.detailedText(farming, goal)
                .contains("GOAL"));
    }

    @Test
    public void farmingHeadsUpUsesConcretePatchAndRepeatLoop()
    {
        GameData data = data(rfdEarlyStatuses(false));
        Recommendation base = skillRecommendation(
                Skill.FARMING, 1, 10, 50.0, "farming_falador_potatoes");
        Guidance guidance = new VariableMethodGuidanceService()
                .build(data, Skill.FARMING, 1, 10,
                        base.getTrainingPlan(), true);
        Recommendation farming = new Recommendation(base.getId(),
                base.getTitle(), base.getReason(), base.getScore(),
                base.getTrainingPlan(), base.getConfidence(), 1, 10, guidance);
        GuidanceChecklist checklist = new MethodGuidanceService(
                TestFixtures.farmingRunPlanner(new FarmingRunCatalog()))
                .build(farming, data);

        assertFalse(checklist.getTitle().equalsIgnoreCase("Farming run"));
        assertTrue(checklist.getWhere().contains("South Falador"));
        assertTrue(checklist.getBring().contains("potato seeds"));
        assertTrue(checklist.getAction().toLowerCase().contains("repeat"));
        assertTrue(checklist.getProgress().contains("1 → 10"));
    }

    @Test
    public void productionSelectorChoosesConcreteLowLevelFarmingMethod()
    {
        RequirementEvidenceEngine evidence = new RequirementEvidenceEngine(
                new FarmingAccessEvaluator(new FarmingAccessCatalog()), null,
                new FarmingSupplyCatalog(), new RunecraftSupplyCatalog());
        TrainingMethodSelector selector = new TrainingMethodSelector(
                new TrainingMethodCatalog(), evidence,
                new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(),
                new UimInventoryResolutionService());
        GameData data = readyFarmingData();
        TrainingPlan plan = selector.select(data, Skill.FARMING, 1,
                StrategyMode.EFFICIENT, SessionIntent.PICK_FOR_ME,
                false, true);
        Guidance guidance = TestFixtures.recommendationGuidanceService()
                .build(data, Skill.FARMING, 1, 10, plan, true);

        assertEquals("farming_falador_potatoes", plan.getMethod().getId());
        assertEquals("Falador potato allotments", plan.getMethod().getName());
        assertTrue(guidance.getLocation().contains("South Falador"));
        assertTrue(guidance.getAction().toLowerCase().contains("repeat"));
    }

    @Test
    public void alternativesUseTwoCompactLinesAtSidebarWidth()
    {
        Recommendation alternative = skillRecommendation(
                Skill.RUNECRAFT, 9, 10, 30.0, "runecraft_earth");
        String text = OsrsStrategistPanel.alternativeText(alternative);
        String[] lines = text.split("\\n");
        assertEquals(2, lines.length);
        assertTrue(lines[0].contains("9 → 10"));
        assertTrue(lines[0].length() <= 32);
        assertTrue(lines[1].length() <= 58);
        assertFalse(text.contains(" — "));
    }

    private static Recommendation named(
            List<Recommendation> candidates, String text)
    {
        return candidates.stream().filter(value -> value.getTitle().contains(text))
                .findFirst().orElseThrow(AssertionError::new);
    }

    private static Recommendation skillRecommendation(Skill skill,
            int current, int target, double score, String methodId)
    {
        TrainingMethod method = new TrainingMethod(methodId, skill, 1, 99,
                methodId.equals("farming_falador_potatoes")
                        ? "Falador potato allotments" : skill.getName() + " method",
                "Use the concrete method.", 10, 10, 10,
                AttentionLevel.LOW, 10, 2, Collections.emptyList(),
                Confidence.VERIFIED, true, false, false);
        TrainingPlan plan = new TrainingPlan(method, "Test plan",
                Confidence.VERIFIED, Collections.emptyList());
        return new Recommendation("skill:" + skill.name().toLowerCase(),
                "Train " + skill.getName() + " to " + target,
                "General progression.", score, plan,
                Confidence.VERIFIED, current, target,
                new Guidance("Repeat the method to the target.",
                        "Bring the method supplies.", "Verified location.", null));
    }

    private static StrategyContext context(GoalType goal,
            QuestTolerance tolerance, Map<String, QuestStatus> statuses)
    {
        return new StrategyContext(data(statuses), StrategyMode.EFFICIENT,
                SessionIntent.PICK_FOR_ME, tolerance, goal, true,
                false, false, new PreferenceProfile());
    }

    private static GameData data(Map<String, QuestStatus> statuses)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            int level = skill == Skill.HITPOINTS ? 10 : 1;
            levels.put(skill, level);
            xp.put(skill, level <= 1 ? 0 : Experience.getXpForLevel(level));
        }
        AccountSnapshot account = new AccountSnapshot("Live GIM", 0L, 4, "GROUP_IRONMAN", Membership.P2P, 1, levels.size(), 0, levels, xp);
        return GameData.builder(account)
                .quests(new QuestSnapshot(statuses))
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .build();
    }

    private static GameData readyFarmingData()
    {
        GameData base = data(rfdEarlyStatuses(false));
        Map<String, Capability> tools = new java.util.HashMap<>();
        tools.put("rake", Capability.VERIFIED);
        tools.put("dibber", Capability.VERIFIED);
        tools.put("spade", Capability.VERIFIED);
        return GameData.builder(base.account())
                .quests(base.quests())
                .farming(new FarmingSnapshot(
                        Collections.singleton("falador"), tools,
                        Collections.emptyMap()))
                .bank(new ItemsState(Collections.singletonList(
                        new ItemState(ItemID.POTATO_SEED,
                                "Potato seed", 3)), 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .build();
    }

    private static Map<String, QuestStatus> rfdEarlyStatuses(boolean goblinComplete)
    {
        Map<String, QuestStatus> result = new LinkedHashMap<>();
        result.put("Recipe for Disaster - Wartface & Bentnoze",
                goblinComplete ? QuestStatus.COMPLETE : QuestStatus.NOT_STARTED);
        result.put("Recipe for Disaster - Another Cook's Quest",
                QuestStatus.NOT_STARTED);
        result.put("Cook's Assistant", QuestStatus.NOT_STARTED);
        result.put("Goblin Diplomacy", QuestStatus.NOT_STARTED);
        return result;
    }

    private static Map<String, QuestStatus> allQuestsIncomplete()
    {
        Map<String, QuestStatus> result = new LinkedHashMap<>();
        for (Quest quest : Quest.values())
            result.put(quest.getName(), QuestStatus.NOT_STARTED);
        return result;
    }
}
