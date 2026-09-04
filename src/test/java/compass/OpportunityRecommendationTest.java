package compass;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OpportunityRecommendationTest
{
    @Test
    public void readyOpportunityCanBeatLowValueXpAndCooldownHidesIt()
    {
        PreferenceProfile preferences = new PreferenceProfile();
        StrategyContext context = context(account(Membership.P2P), preferences);
        StrategyEngine engine = engine();
        Opportunity opportunity = new Opportunity("opportunity:battlestaves",
                OpportunityType.BATTLESTAVES, "Daily battlestaves", true,
                Confidence.VERIFIED, Collections.emptyList(), true);
        Recommendation promoted = engine.opportunityRecommendation(opportunity, context);
        Recommendation lowXp = new Recommendation("skill:mining", "Mine", "test", 1,
                null, Confidence.VERIFIED, 1, 10,
                new Guidance("Mine.", "Pickaxe.", "Mine.", "test"),
                Safety.skill(true, Skill.MINING));

        List<Recommendation> queue = engine.buildPlayerQueue(Arrays.asList(lowXp, promoted), context);
        assertEquals("opportunity:battlestaves", queue.get(0).getId());

        preferences.apply("opportunity:battlestaves", FeedbackAction.NOT_TODAY);
        assertNull(engine.opportunityRecommendation(opportunity, context));
    }

    @Test
    public void unresolvedOpportunityCannotBePromoted()
    {
        Opportunity unresolved = new Opportunity("opportunity:herb-run",
                OpportunityType.HERB_RUN, "Herb run", false,
                Confidence.CHECK_NEEDED, Collections.emptyList());
        assertNull(engine().opportunityRecommendation(unresolved,
                context(account(Membership.P2P), new PreferenceProfile())));
    }

    @Test
    public void readyHerbTimerWithoutVerifiedPatchCannotBePromoted()
    {
        StrategyEngine engine = engine();
        Recommendation preparation = engine.opportunityRecommendation(
                new Opportunity("opportunity:herb-run", OpportunityType.HERB_RUN,
                        "Herb run", true, Confidence.VERIFIED,
                        Collections.singletonList("Seeds")),
                context(account(Membership.P2P), new PreferenceProfile()));
        assertNull(preparation);
    }

    @Test
    public void readyHerbRunNamesTheVerifiedPatchRoute()
    {
        GameData data = GameData.builder(
                        account(Membership.P2P))
                .farming(new FarmingSnapshot(
                        Collections.singleton("falador"),
                        Collections.emptyMap(), Collections.emptyMap()))
                .build();
        StrategyContext context = new StrategyContext(data,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.MAX, false, false, false,
                new PreferenceProfile());
        Recommendation recommendation = engine().opportunityRecommendation(
                new Opportunity("opportunity:herb-run",
                        OpportunityType.HERB_RUN, "Herb run", true,
                        Confidence.VERIFIED,
                        Collections.emptyList(), true), context);

        assertNotNull(recommendation);
        assertEquals("Falador patches.",
                recommendation.getGuidance().getLocation());
        assertTrue(new ActionabilityPolicy()
                .canLeadQueue(recommendation));
    }

    @Test
    public void evaluateAppliesFeedbackAndRemovesEveryPromotedDuplicate()
    {
        Opportunity verified = new Opportunity("opportunity:battlestaves",
                OpportunityType.BATTLESTAVES, "Daily battlestaves", true,
                Confidence.VERIFIED, Collections.emptyList(), true);
        OpportunityEngine opportunities = new OpportunityEngine()
        {
            @Override
            public List<Opportunity> evaluate(GameData data)
            {
                return Collections.singletonList(verified);
            }
        };
        StrategyEngine engine = TestFixtures.strategyEngine(null, opportunities, null, null,
                new ActionabilityPolicy());
        GameData data = GameData.builder(
                account(Membership.P2P)).build();

        PreferenceProfile preferences = new PreferenceProfile();
        StrategyResult visible = engine.evaluate(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, preferences);
        assertEquals("opportunity:battlestaves",
                visible.getRecommendations().get(0).getId());
        assertTrue(visible.getOpportunities().isEmpty());

        for (FeedbackAction action : Arrays.asList(FeedbackAction.LATER,
                FeedbackAction.NOT_TODAY, FeedbackAction.DISLIKE))
        {
            PreferenceProfile hidden = new PreferenceProfile();
            hidden.apply(verified.getId(), action);
            StrategyResult result = engine.evaluate(data, StrategyMode.BALANCED,
                    SessionIntent.PICK_FOR_ME, hidden);
            assertTrue(FallbackRecommendationFactory.isFallback(
                    result.getRecommendations().get(0)));
            assertTrue(result.getOpportunities().isEmpty());
        }

    }

    @Test
    public void opportunityInSecondaryQueueSlotIsNotDuplicatedInSidebar()
    {
        Opportunity verified = new Opportunity("opportunity:battlestaves",
                OpportunityType.BATTLESTAVES, "Daily battlestaves", true,
                Confidence.VERIFIED, Collections.emptyList(), true);
        OpportunityEngine opportunityEngine = new OpportunityEngine()
        {
            @Override public List<Opportunity> evaluate(GameData data)
            { return Collections.singletonList(verified); }
        };
        RecommendationEngine recommendationEngine = new RecommendationEngine((TrainingMethodSelector) null,
                TestFixtures.recommendationGuidanceService(),
                null, null, null, null, null)
        {
            @Override
            public List<Recommendation> recommendAll(GameData data,
                    StrategyMode mode, SessionIntent intent, boolean groupStorage,
                    boolean wilderness, PreferenceProfile preferences)
            {
                return Arrays.asList(ready("skill:mining", 100),
                        ready("skill:fishing", 90));
            }
        };
        StrategyEngine engine = TestFixtures.strategyEngine(recommendationEngine,
                opportunityEngine, null, null,
                new ActionabilityPolicy());
        StrategyResult result = engine.evaluate(GameData.builder(
                        account(Membership.P2P)).build(),
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                new PreferenceProfile());
        assertTrue(result.getRecommendations().stream().anyMatch(value ->
                verified.getId().equals(value.getId())));
        assertTrue(result.getOpportunities().isEmpty());
    }

    @Test
    public void timerOnlyOpportunityStaysOutOfGlobalQueue()
    {
        Map<String, Long> timers = new java.util.HashMap<>();
        timers.put("opportunity:battlestaves", 0L);
        GameData data = GameData.builder(
                        account(Membership.P2P))
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers)).build();
        StrategyResult result = TestFixtures.strategyEngine(null, new OpportunityEngine(),
                null, null, new ActionabilityPolicy())
                .evaluate(data, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                        new PreferenceProfile());
        assertTrue(FallbackRecommendationFactory.isFallback(
                result.getRecommendations().get(0)));
        assertTrue(result.getOpportunities().stream().anyMatch(value ->
                "opportunity:battlestaves".equals(value.getId())));
    }

    private static Recommendation ready(String id, double score)
    {
        return new Recommendation(id, id, "test", score, null,
                Confidence.VERIFIED, 0, 0,
                new Guidance("Do it.", "Ready.", "Here.", "Test."),
                Safety.skill(true, id.contains("mining")
                        ? Skill.MINING : Skill.FISHING));
    }

    private static StrategyEngine engine()
    {
        return TestFixtures.strategyEngine(null, null, null, null,
                new ActionabilityPolicy());
    }

    private static StrategyContext context(AccountSnapshot account, PreferenceProfile preferences)
    {
        return new StrategyContext(GameData.builder(account).build(),
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.MAX, false, false, false, preferences);
    }

    private static AccountSnapshot account(Membership membership)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 50); xp.put(skill, 0); }
        return new AccountSnapshot("Opportunity", 0L, 0, "Main", membership, 1, 1000, 0L, levels, xp);
    }
}
