package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReviewedBankingPolicyTest
{
    private final TrainingMethodCatalog methods = new TrainingMethodCatalog();
    private final MethodStrategyKnowledgeCatalog strategy = new MethodStrategyKnowledgeCatalog();

    @Test
    public void reviewedLocalProcessingReplacesLegacyBankLoopForUim()
    {
        CuratedTrainingMethod bows = method(Skill.FLETCHING, "fletching_bows");
        assertNull(strategy.profileFor(bows.method(), bows.getMetadata(), AccountMode.ULTIMATE_IRONMAN));
        MethodStrategyProfile reviewed = strategy.profileFor(bows.method(), bows.getMetadata(),
                AccountMode.ULTIMATE_IRONMAN, BankingMode.LOCAL_PROCESSING);
        assertNotNull(reviewed);
        assertTrue(reviewed.supports(AccountMode.ULTIMATE_IRONMAN));
        assertEquals(BankingMode.LOCAL_PROCESSING, reviewed.bankingBehavior);
        assertNull(strategy.profileFor(bows.method(), bows.getMetadata(),
                AccountMode.ULTIMATE_IRONMAN, BankingMode.CONVENTIONAL_BANK_LOOP));
    }

    @Test
    public void reviewedConventionalLoopStillFailsClosedForUim()
    {
        CuratedTrainingMethod wines = method(Skill.COOKING, "cooking_wines");
        assertNull(strategy.profileFor(wines.method(), wines.getMetadata(),
                AccountMode.ULTIMATE_IRONMAN, BankingMode.CONVENTIONAL_BANK_LOOP));
    }

    @Test
    public void fullSelectorUsesReviewedBankingWithoutChangingLegacyMethodPolicy()
    {
        List<ItemState> inventory = MethodIntelligenceTest.carried(1511, "Logs", 4);
        inventory.add(new ItemState(946, "Knife", 1));
        GameData uim = MethodIntelligenceTest.data(2, -1, inventory, Collections.emptyList());
        TrainingMethodSelector selector = new TrainingMethodSelector(methods, null,
                new TrainingMethodPolicy(), strategy, new UimInventoryResolutionService(),
                new MethodReadinessService(new MethodIntelligenceCatalog(),
                        new MethodPreparationService(MethodIntelligenceTest.resources(0, 0))));
        TrainingPlan bows = selector.rankedCandidates(uim, Skill.FLETCHING, 80,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false).stream()
                .filter(p -> p.method().id.equals("fletching_bows") && p.readiness.actionable())
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(BankingMode.LOCAL_PROCESSING, bows.strategyProfile.bankingBehavior);
        assertEquals("longbow_cutting", bows.readiness.recipe.id);
        assertTrue(selector.rankedCandidates(uim, Skill.COOKING, 80,
                StrategyMode.EFFICIENT, SessionIntent.ONE_HOUR, false, false).stream()
                .noneMatch(p -> p.method().id.equals("cooking_wines")));
    }

    @Test
    public void carriedLogsAndTinderboxEnableLocalUimFiremaking()
    {
        List<ItemState> inventory = MethodIntelligenceTest.carried(
                1511, "Logs", 4);
        inventory.add(new ItemState(590, "Tinderbox", 1));
        GameData uim = MethodIntelligenceTest.data(
                2, -1, inventory, Collections.emptyList());
        TrainingMethodSelector selector = new TrainingMethodSelector(
                methods, null, new TrainingMethodPolicy(), strategy,
                new UimInventoryResolutionService(),
                new MethodReadinessService(new MethodIntelligenceCatalog(),
                        new MethodPreparationService(
                                MethodIntelligenceTest.resources(0, 0))));

        TrainingPlan firemaking = selector.rankedCandidates(
                uim, Skill.FIREMAKING, 80, StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, false, false).stream()
                .filter(plan -> plan.method().id.equals(
                        "firemaking_f2p_logs")
                        && plan.readiness.actionable())
                .findFirst().orElseThrow(AssertionError::new);

        RecommendationEngine recommendations =
                TestFixtures.recommendationEngine(selector);
        ActionabilityPolicy actionability = new ActionabilityPolicy();
        for (StrategyMode mode : StrategyMode.values())
        {
            for (SessionIntent session : Arrays.asList(
                    SessionIntent.QUICK_20_MIN,
                    SessionIntent.ONE_HOUR,
                    SessionIntent.LONG_SESSION,
                    SessionIntent.AFK))
            {
                Recommendation selected = recommendations.recommendAll(
                        uim, mode, session, false, false,
                        GoalType.AUTOMATIC, new PreferenceProfile()).stream()
                        .filter(candidate -> candidate.getTrainingPlan()
                                != null
                                && candidate.getTrainingPlan().getMethod()
                                != null
                                && candidate.getTrainingPlan().getMethod()
                                        .getSkill() == Skill.FIREMAKING)
                        .findFirst().orElseThrow(AssertionError::new);
                assertEquals(mode + " " + session,
                        "firemaking_f2p_logs", selected.getTrainingPlan()
                                .getMethod().getId());
                assertTrue(mode + " " + session,
                        actionability.canLeadQueue(selected));
            }
        }

        assertEquals(BankingMode.LOCAL_PROCESSING,
                firemaking.strategyProfile.bankingBehavior);
        assertEquals("normal_logs_firemaking",
                firemaking.readiness.recipe.id);
        Guidance guidance = firemaking.readiness.guidance(81);
        assertTrue(guidance.getAction().contains(
                "Use the tinderbox on the prepared normal logs"));
        assertFalse(guidance.getAction().toLowerCase(
                Locale.ROOT).contains("bank"));

        GameData missingTool = MethodIntelligenceTest.data(2, -1,
                MethodIntelligenceTest.carried(1511, "Logs", 4),
                Collections.emptyList());
        assertTrue(selector.rankedCandidates(missingTool,
                Skill.FIREMAKING, 80, StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, false, false).stream()
                .filter(plan -> plan.method().id.equals(
                        "firemaking_f2p_logs"))
                .noneMatch(plan -> plan.readiness.actionable()));
    }

    @Test
    public void carriedNormalBonesEnableLocalUimPrayer()
    {
        GameData uim = MethodIntelligenceTest.data(2, -1,
                MethodIntelligenceTest.carried(526, "Bones", 4),
                Collections.emptyList());
        TrainingMethodSelector selector = new TrainingMethodSelector(
                methods, null, new TrainingMethodPolicy(), strategy,
                new UimInventoryResolutionService(),
                new MethodReadinessService(new MethodIntelligenceCatalog(),
                        new MethodPreparationService(
                                MethodIntelligenceTest.resources(0, 0))));
        RecommendationEngine recommendations =
                TestFixtures.recommendationEngine(selector);
        ActionabilityPolicy actionability = new ActionabilityPolicy();

        for (StrategyMode mode : StrategyMode.values())
        {
            for (SessionIntent session : Arrays.asList(
                    SessionIntent.QUICK_20_MIN,
                    SessionIntent.ONE_HOUR,
                    SessionIntent.LONG_SESSION,
                    SessionIntent.AFK))
            {
                Recommendation selected = recommendations.recommendAll(
                        uim, mode, session, false, false,
                        GoalType.AUTOMATIC, new PreferenceProfile()).stream()
                        .filter(candidate -> candidate.getTrainingPlan()
                                != null
                                && candidate.getTrainingPlan().getMethod()
                                != null
                                && candidate.getTrainingPlan().getMethod()
                                        .getSkill() == Skill.PRAYER)
                        .findFirst().orElseThrow(AssertionError::new);
                assertEquals(mode + " " + session,
                        "prayer_f2p_bones", selected.getTrainingPlan()
                                .getMethod().getId());
                assertTrue(mode + " " + session,
                        actionability.canLeadQueue(selected));
                assertFalse(selected.getGuidance().getAction().toLowerCase(
                        Locale.ROOT).contains("bank"));
            }
        }

        GameData missingBones = MethodIntelligenceTest.data(
                2, -1, Collections.emptyList(), Collections.emptyList());
        assertTrue(selector.rankedCandidates(missingBones,
                Skill.PRAYER, 80, StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, false, false).stream()
                .filter(plan -> plan.method().id.equals(
                        "prayer_f2p_bones"))
                .noneMatch(plan -> plan.readiness.actionable()));
    }

    private CuratedTrainingMethod method(Skill skill, String id)
    {
        return methods.curatedFor(skill).stream().filter(m -> m.method().id.equals(id))
                .findFirst().orElseThrow(AssertionError::new);
    }
}
