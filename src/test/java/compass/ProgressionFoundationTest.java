package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProgressionFoundationTest
{
    @Test
    public void f2pIronGetsAnAvailableStepAndAReviewedBlockedNextUnlock()
    {
        List<Recommendation> ranked = engine().recommendAll(data(1, 67, false),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false,
                GoalType.AUTOMATIC, new PreferenceProfile());
        Recommendation recommendation = smithing(ranked);
        assertNotNull(recommendation);
        assertSame(recommendation, ranked.get(0));
        assertEquals("Train Smithing to 68", recommendation.title);
        assertEquals(0.35, recommendation.strategicValue.unlockValue, 0);
        assertTrue(recommendation.strategicValue.hasTypedEvidence());
        assertEquals("steel_platebody_smithing", recommendation.plan().readiness.recipe.id);
        assertTrue(recommendation.guidance.getAction().contains("Steel platebody"));

        MethodIntelligenceCatalog.Recipe blocked = recommendation.plan().readiness.nextUnlock;
        assertNotNull(blocked);
        assertEquals("mithril_platebody_smithing", blocked.id);
        assertEquals(68, blocked.level);
        String details = Presentation.detailedText(recommendation);
        assertTrue(details, details.contains("NEXT UNLOCK"));
        assertTrue(details, details.contains("Mithril platebody becomes available at Smithing 68"));
        assertTrue(details, details.contains("Current level: 67"));
        assertTrue(details, details.contains("excluded until that requirement is met"));
    }

    @Test
    public void reachingTheReviewedRequirementMakesTheBlockedRecipeWin()
    {
        Recommendation recommendation = smithing(engine().recommendAll(data(1, 68, false),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false,
                GoalType.AUTOMATIC, new PreferenceProfile()));
        assertNotNull(recommendation);
        assertEquals("mithril_platebody_smithing", recommendation.plan().readiness.recipe.id);
        assertTrue(recommendation.guidance.getAction().contains("Mithril platebody"));
    }

    @Test
    public void lockedHighTierResourcesDoNotChangeTheCurrentWinnerOrDeterminism()
    {
        Recommendation first = smithing(engine().recommendAll(data(1, 67, false),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false,
                GoalType.AUTOMATIC, new PreferenceProfile()));
        Recommendation withRunite = smithing(engine().recommendAll(data(1, 67, true),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false,
                GoalType.AUTOMATIC, new PreferenceProfile()));
        Recommendation repeated = smithing(engine().recommendAll(data(1, 67, false),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false,
                GoalType.AUTOMATIC, new PreferenceProfile()));
        assertEquals(first.plan().readiness.recipe.id, withRunite.plan().readiness.recipe.id);
        assertEquals(first.plan().readiness.recipe.id, repeated.plan().readiness.recipe.id);
        assertEquals(first.plan().readiness.nextUnlock.id, repeated.plan().readiness.nextUnlock.id);
        assertEquals(first.score, repeated.score, 0);
    }

    @Test
    public void uimDoesNotReceiveTheConventionalBankLoop()
    {
        GameData data = data(2, 67, false);
        TrainingMethodSelector selector = selector();
        assertTrue(selector.rankedCandidates(data, Skill.SMITHING, 67,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false).stream()
                .noneMatch(plan -> plan.method().id.equals("smithing_f2p_platebodies")));
    }

    @Test
    public void unrelatedFletchingWinnerIsPreserved()
    {
        List<ItemState> inventory = MethodIntelligenceTest.carried(1511, "Logs", 4);
        inventory.addAll(MethodIntelligenceTest.carried(1515, "Yew logs", 6));
        inventory.add(new ItemState(946, "Knife", 1));
        TrainingPlan selected = selector().rankedCandidates(
                MethodIntelligenceTest.data(1, -1, inventory, Collections.emptyList()),
                Skill.FLETCHING, 80, StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, false, false).stream()
                .filter(plan -> plan.method().id.equals("fletching_bows")
                        && plan.readiness.actionable()).findFirst().orElseThrow(AssertionError::new);
        assertEquals("yew_longbow_cutting", selected.readiness.recipe.id);
    }

    @Test
    public void reviewedFletchingRecipesReuseTheGenericProgressionPath()
    {
        Recommendation before = fletching(engine().recommendAll(fletchingData(69),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false,
                GoalType.AUTOMATIC, new PreferenceProfile()));
        assertNotNull(before);
        assertEquals("Train Fletching to 70", before.title);
        assertEquals("yew_shortbow_cutting", before.plan().readiness.recipe.id);
        assertEquals("yew_longbow_cutting", before.plan().readiness.nextUnlock.id);
        assertTrue(before.strategicValue.getEvidenceIds()
                .contains("method-recipe:yew_longbow_cutting"));
        String details = Presentation.detailedText(before);
        assertTrue(details, details.contains("NEXT UNLOCK"));
        assertTrue(details, details.contains("Yew longbow (u) becomes available at Fletching 70"));

        Recommendation unlocked = fletching(engine().recommendAll(fletchingData(70),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false,
                GoalType.AUTOMATIC, new PreferenceProfile()));
        assertNotNull(unlocked);
        assertEquals("yew_longbow_cutting", unlocked.plan().readiness.recipe.id);
    }

    private static RecommendationEngine engine()
    { return TestFixtures.recommendationEngine(selector()); }

    private static TrainingMethodSelector selector()
    {
        return new TrainingMethodSelector(new TrainingMethodCatalog(), null,
                new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(),
                new UimInventoryResolutionService());
    }

    private static Recommendation smithing(List<Recommendation> recommendations)
    {
        return recommendations.stream().filter(r -> r.plan() != null
                && r.plan().method().getSkill() == Skill.SMITHING).findFirst().orElse(null);
    }

    private static Recommendation fletching(List<Recommendation> recommendations)
    {
        return recommendations.stream().filter(r -> r.plan() != null
                && r.plan().method().getSkill() == Skill.FLETCHING).findFirst().orElse(null);
    }

    private static GameData fletchingData(int fletchingLevel)
    {
        List<ItemState> inventory = MethodIntelligenceTest.carried(1515, "Yew logs", 6);
        inventory.add(new ItemState(946, "Knife", 1));
        GameData source = MethodIntelligenceTest.data(1, -1,
                inventory, Collections.emptyList());
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
            levels.put(skill, skill == Skill.FLETCHING ? fletchingLevel : 99);
        AccountSnapshot old = source.account();
        AccountSnapshot account = new AccountSnapshot("fletching-progression-test", 2, 1,
                old.accountTypeName, Membership.P2P, 1, 1, 0,
                levels, Collections.emptyMap());
        return GameData.builder(account).inventory(source.inventory()).equipment(source.equipment())
                .bank(source.bank()).economy(source.economy()).build();
    }

    private static GameData data(int mode, int smithingLevel, boolean runite)
    {
        List<ItemState> bank = MethodIntelligenceTest.carried(2353, "Steel bar", 25);
        bank.addAll(MethodIntelligenceTest.carried(2359, "Mithril bar", 25));
        bank.add(new ItemState(2347, "Hammer", 1));
        if (runite) bank.addAll(MethodIntelligenceTest.carried(2363, "Runite bar", 25));
        GameData source = MethodIntelligenceTest.data(mode, -1,
                Collections.emptyList(), bank);
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) levels.put(skill, skill == Skill.SMITHING ? smithingLevel : 99);
        AccountSnapshot old = source.account();
        AccountSnapshot account = new AccountSnapshot("progression-test", 1, mode,
                old.accountTypeName, Membership.F2P, 0, 1, 0, levels, Collections.emptyMap());
        return GameData.builder(account).inventory(source.inventory()).equipment(source.equipment())
                .bank(source.bank()).economy(source.economy()).build();
    }
}
