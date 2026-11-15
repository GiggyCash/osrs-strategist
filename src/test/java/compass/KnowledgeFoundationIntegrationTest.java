package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class KnowledgeFoundationIntegrationTest
{
    @Test
    public void reviewedWikiLevelBoundaryChangesThePlayerFacingSmithingRecommendation()
    {
        TrainingPlan steel = smithingPlan(data(67));
        assertEquals("steel_platebody_smithing", steel.readiness.recipe.id);
        assertTrue(steel.readiness.guidance(68).getAction().contains("Steel platebody"));

        TrainingPlan mithril = smithingPlan(data(68)).withCurrentStageTargetLevel(69);
        assertEquals("mithril_platebody_smithing", mithril.readiness.recipe.id);
        assertNotNull(mithril.comparison);
        assertTrue(mithril.comparison.alternative.contains("Steel platebody"));
        Guidance guidance = mithril.readiness.guidance(69);
        Recommendation recommendation = new Recommendation("skill:smithing", "Train Smithing",
                "Reviewed Smithing route.", 20, mithril, Confidence.VERIFIED, 68, 69, guidance);
        assertTrue(recommendation.guidance.getAction().contains("Mithril platebody"));
        assertTrue(recommendation.plan().comparison.reason.contains("Compared with"));
    }

    private static TrainingPlan smithingPlan(GameData data)
    {
        TrainingMethodSelector selector = new TrainingMethodSelector(new TrainingMethodCatalog(), null,
                new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(),
                new UimInventoryResolutionService());
        return selector.rankedCandidates(data, Skill.SMITHING, data.account().level(Skill.SMITHING),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false).stream()
                .filter(plan -> plan.method().id.equals("smithing_f2p_platebodies")
                        && plan.readiness != null && plan.readiness.actionable())
                .findFirst().orElseThrow(AssertionError::new);
    }

    private static GameData data(int smithingLevel)
    {
        List<ItemState> inventory = Collections.singletonList(new ItemState(2347, "Hammer", 1));
        List<ItemState> bank = MethodIntelligenceTest.carried(2353, "Steel bar", 25);
        bank.addAll(MethodIntelligenceTest.carried(2359, "Mithril bar", 25));
        GameData source = MethodIntelligenceTest.data(0, -1, inventory, bank);
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) levels.put(skill, skill == Skill.SMITHING ? smithingLevel : 80);
        AccountSnapshot old = source.account();
        AccountSnapshot account = new AccountSnapshot("knowledge-test", 1, old.modeCode(),
                old.accountTypeName, Membership.F2P, 0, 1, 0, levels, Collections.emptyMap());
        return GameData.builder(account).inventory(source.inventory()).equipment(source.equipment())
                .bank(source.bank()).economy(source.economy()).build();
    }
}
