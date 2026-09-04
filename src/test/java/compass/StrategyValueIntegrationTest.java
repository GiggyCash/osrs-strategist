package compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** Cross-seam tests for typed value attachment and provider ownership. */
public class StrategyValueIntegrationTest
{
    @Test
    public void methodTravelEvidenceChangesRenderedLocationAndTypedValue()
    {
        TrainingMethod method = method("farming_fruit_trees", Skill.FARMING);
        Recommendation recommendation = recommendation(
                "skill:farming", method, 27, 33);
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("The Grand Tree", QuestStatus.COMPLETE);
        StrategyContext context = context(account("MAIN"),
                GameData.builder(account("MAIN"))
                        .quests(new QuestSnapshot(quests)).build());

        Recommendation valued = new MethodRecommendationValueService()
                .attach(recommendation, context);

        assertEquals("Tree Gnome Stronghold fruit tree patch.",
                valued.getGuidance().getLocation());
        assertTrue(valued.getStrategicValue().getTravelFit() > 0.0);
        assertTrue(valued.getStrategicValue().getEvidenceIds().stream()
                .anyMatch(value -> value.startsWith("travel:")));
    }

    @Test
    public void finalQueueUsesTypedValueWithoutInspectingIdentityOrProse()
    {
        Recommendation ordinary = ready("candidate:ordinary", 50.0,
                StrategicValue.neutral());
        Recommendation valuable = ready("candidate:opaque", 43.0,
                StrategicValue.builder()
                        .unlockValue(1.0)
                        .evidence("unlock:verified")
                        .build());
        StrategyContext context = context(account("MAIN"),
                GameData.builder(account("MAIN")).build());

        List<Recommendation> queue = engine(null, null).buildPlayerQueue(
                Arrays.asList(ordinary, valuable), context);

        assertEquals("candidate:opaque", queue.get(0).getId());
    }

    @Test
    public void freshEnabledGroupStockAddsExactResourceEvidence()
    {
        AccountSnapshot account = account(4, "GROUP_IRONMAN",
                Skill.FIREMAKING, 1);
        ItemsState group = new ItemsState(true,
                Collections.singletonList(new ItemState(
                        ItemID.LOGS, "Logs", 20)));
        GameData data = GameData.builder(account)
                .groupStorage(group).build();
        StrategyContext context = new StrategyContext(data,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.AUTOMATIC, true, false,
                new PreferenceProfile());
        Recommendation recommendation = recommendation("skill:firemaking",
                method("firemaking_f2p_logs", Skill.FIREMAKING), 1, 2);

        RuneLiteSkillActionCatalog liveActions =
                new RuneLiteSkillActionCatalog()
                {
                    @Override
                    public List<ActionDef> actionsFor(
                            Skill skill)
                    {
                        return Collections.singletonList(
                                new ActionDef(
                                        Skill.FIREMAKING,
                                        "runelite:firemaking:logs", "Logs",
                                        1, 40.0f, null,
                                        Membership.F2P, ItemID.LOGS));
                    }
                };
        Recommendation valued = new MethodResourceValueService(liveActions)
                .attach(recommendation, context);

        assertTrue(valued.getStrategicValue().getEvidenceIds().toString(),
                valued.getStrategicValue().getEvidenceIds().stream()
                .anyMatch(value -> value.equals("group-resource:logs")));
        assertTrue(valued.getStrategicValue().getAccountModeFit() > 0.0);
    }

    @Test
    public void providerSupersedesGenericCandidateOnlyWhenItEmitsReplacement()
    {
        Recommendation generic = ready("skill:slayer", 70.0,
                StrategicValue.neutral());
        RecommendationEngine recommendations = recommendationEngine(generic);
        CandidateProvider emptyOwner = provider(
                Collections.emptyList(), Collections.singleton("skill:slayer"));
        CandidateProvider replacementOwner = provider(
                Collections.singletonList(new Recommendation(
                        "slayer:get-task", "Get a task", "Observed no-task state.",
                        60.0, Confidence.VERIFIED, guidance(),
                        Safety.verifiedSafe(false))),
                Collections.singleton("skill:slayer"));
        GameData data = GameData.builder(account("MAIN"))
                .build();

        List<Recommendation> fallback = engine(recommendations, emptyOwner)
                .evaluate(data, StrategyMode.BALANCED,
                        SessionIntent.PICK_FOR_ME, new PreferenceProfile())
                .getRecommendations();
        List<Recommendation> replaced = engine(recommendations, replacementOwner)
                .evaluate(data, StrategyMode.BALANCED,
                        SessionIntent.PICK_FOR_ME, new PreferenceProfile())
                .getRecommendations();

        assertTrue(fallback.stream().anyMatch(value ->
                "skill:slayer".equals(value.getId())));
        assertTrue(replaced.stream().anyMatch(value ->
                "slayer:get-task".equals(value.getId())));
        assertFalse(replaced.stream().anyMatch(value ->
                "skill:slayer".equals(value.getId())));
    }

    private static StrategyEngine engine(RecommendationEngine recommendations,
            CandidateProvider provider)
    {
        StrategyCandidateRegistry registry = provider == null ? null
                : new StrategyCandidateRegistry(
                        Collections.singletonList(provider));
        return TestFixtures.strategyEngine(recommendations, null, null, registry,
                new ActionabilityPolicy(),
                new RecommendationIntelligenceService());
    }

    private static CandidateProvider provider(
            List<Recommendation> candidates, Set<String> superseded)
    {
        return new CandidateProvider()
        {
            @Override public String getId() { return "test-owner"; }
            @Override public List<Recommendation> candidates(
                    StrategyContext context) { return candidates; }
            @Override public Set<String> supersededCandidateIds()
            { return superseded; }
        };
    }

    private static RecommendationEngine recommendationEngine(
            Recommendation recommendation)
    {
        return new RecommendationEngine((TrainingMethodSelector) null,
                TestFixtures.recommendationGuidanceService(),
                null, null, null, null, null)
        {
            @Override
            public List<Recommendation> recommendAll(
                    GameData data, StrategyMode strategyMode,
                    SessionIntent sessionIntent, boolean useGroupStorage,
                    boolean allowWildernessMethods,
                    PreferenceProfile preferences)
            {
                return Collections.singletonList(recommendation);
            }
        };
    }

    private static Recommendation recommendation(String id,
            TrainingMethod method, int current, int target)
    {
        TrainingPlan plan = new TrainingPlan(method, "test",
                Confidence.VERIFIED,
                Collections.emptyList());
        return new Recommendation(id, "Train Farming", "Test.", 40.0,
                plan, Confidence.VERIFIED, current, target,
                guidance(), Safety.skill(false,
                        method.getSkill()));
    }

    private static Recommendation ready(String id, double score,
            StrategicValue value)
    {
        return new Recommendation(id, id, "Neutral test wording.", score,
                null, Confidence.VERIFIED, 0, 0, guidance(),
                Safety.verifiedSafe(false))
                .withStrategicValue(value);
    }

    private static Guidance guidance()
    {
        return new Guidance("Plant the fruit-tree sapling and repeat the checked route.",
                "Required setup is observed.", "Falador Park.",
                "Typed test evidence.");
    }

    private static TrainingMethod method(String id, Skill skill)
    {
        return new TrainingMethod(id, skill, 1, 99, id, "Do it.",
                10, 10, 10, AttentionLevel.MODERATE, 20, 2,
                Collections.emptyList(), Confidence.VERIFIED,
                true);
    }

    private static StrategyContext context(AccountSnapshot account,
            GameData data)
    {
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false, new PreferenceProfile());
    }

    private static AccountSnapshot account(String type)
    {
        return account(0, type, null, 70);
    }

    private static AccountSnapshot account(int typeCode, String type,
            Skill override, int overrideLevel)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        if (override != null) levels.put(override, overrideLevel);
        return new AccountSnapshot("Integration", 991L, typeCode, type,
                Membership.P2P, 1, 70 * Skill.values().length,
                100, levels, xp);
    }
}
