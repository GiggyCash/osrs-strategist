package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class FoundationPipelineIntegrationTest
{
    @Test
    public void uimBowAlternativesReachRecommendationGuidanceAndExplanation()
    {
        List<ItemState> inventory = MethodIntelligenceTest.carried(1511, "Logs", 4);
        inventory.addAll(MethodIntelligenceTest.carried(1515, "Yew logs", 6));
        inventory.add(new ItemState(946, "Knife", 1));
        Recommendation recommendation = skill(engine(), MethodIntelligenceTest.data(2, -1,
                inventory, Collections.emptyList()), Skill.FLETCHING,
                StrategyMode.RELAXED, SessionIntent.AFK);
        assertNotNull(recommendation);
        assertEquals("fletching_bows", recommendation.plan().method().id);
        assertEquals("yew_longbow_cutting", recommendation.plan().readiness.recipe.id);
        assertEquals(BankingMode.LOCAL_PROCESSING,
                recommendation.plan().strategyProfile.bankingBehavior);
        assertNotNull(recommendation.plan().comparison);
        assertTrue(recommendation.guidance.getAction().contains("Yew longbow"));
        assertTrue(Presentation.detailedText(recommendation).contains("Compared with"));
    }

    @Test
    public void smithingFactsUseTheSamePipelineWithoutNewJavaPolicy()
    {
        List<ItemState> inventory = MethodIntelligenceTest.carried(2353, "Steel bar", 25);
        inventory.add(new ItemState(2347, "Hammer", 1));
        GameData data = withMembership(MethodIntelligenceTest.data(0, -1,
                inventory, Collections.emptyList()), Membership.F2P);
        Recommendation recommendation = skill(engine(), data, Skill.SMITHING,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR);
        assertNotNull(recommendation);
        assertEquals("smithing_f2p_platebodies", recommendation.plan().method().id);
        assertEquals("steel_platebody_smithing", recommendation.plan().readiness.recipe.id);
        assertEquals(5, recommendation.plan().readiness.batch);
        assertTrue(recommendation.guidance.getAction().contains("Steel platebody"));
        assertTrue(recommendation.guidance.getSupplies().contains("Steel bar"));
        assertNotNull(recommendation.plan().comparison);
    }

    private static RecommendationEngine engine()
    {
        TrainingMethodSelector selector = new TrainingMethodSelector(new TrainingMethodCatalog(), null,
                new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(),
                new UimInventoryResolutionService());
        return TestFixtures.recommendationEngine(selector);
    }

    private static Recommendation skill(RecommendationEngine engine, GameData data, Skill skill,
            StrategyMode mode, SessionIntent intent)
    {
        return engine.recommendAll(data, mode, intent, false, false,
                GoalType.AUTOMATIC, new PreferenceProfile()).stream()
                .filter(r -> r.plan() != null && r.plan().method().getSkill() == skill)
                .findFirst().orElse(null);
    }

    private static GameData withMembership(GameData source, Membership membership)
    {
        AccountSnapshot old = source.account();
        AccountSnapshot account = new AccountSnapshot("test", 1, old.modeCode(),
                old.accountTypeName, membership, membership == Membership.P2P ? 1 : 0,
                old.totalLevel, old.totalExperience, old.skillLevels, old.skillExperience);
        return GameData.builder(account).inventory(source.inventory()).equipment(source.equipment())
                .bank(source.bank()).economy(source.economy()).build();
    }
}
