package compass;

import java.util.*;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class CandidateComparisonTest
{
    @Test
    public void selectedPlanExplainsTheClosestAlternativeWithoutLosingEvidence()
    {
        List<ItemState> inventory = MethodIntelligenceTest.carried(1511, "Logs", 4);
        inventory.addAll(MethodIntelligenceTest.carried(1515, "Yew logs", 6));
        inventory.add(new ItemState(946, "Knife", 1));
        List<TrainingPlan> bows = plans(MethodIntelligenceTest.data(1, -1,
                inventory, Collections.emptyList())).stream()
                .filter(p -> p.method().id.equals("fletching_bows") && p.readiness.actionable())
                .collect(Collectors.toList());
        TrainingPlan selected = bows.get(0);
        assertEquals("yew_longbow_cutting", selected.readiness.recipe.id);
        assertNotNull(selected.comparison);
        assertTrue(selected.comparison.alternative.contains("Yew shortbow"));
        assertEquals(CandidateComparison.Basis.CATALOG_ORDER, selected.comparison.basis);
        assertTrue(selected.comparison.reason.contains("deterministic catalog order"));
        assertSame(selected.readiness, selected.withCurrentStageTargetLevel(85).readiness);
        assertSame(selected.comparison, selected.withCurrentStageTargetLevel(85).comparison);
    }

    @Test
    public void preparationAndUnresolvedRequirementsProduceTruthfulReasons()
    {
        List<ItemState> carried = MethodIntelligenceTest.carried(1511, "Logs", 4);
        carried.add(new ItemState(946, "Knife", 1));
        List<TrainingPlan> plans = plans(MethodIntelligenceTest.data(1, -1, carried,
                MethodIntelligenceTest.carried(1515, "Yew logs", 6)));
        TrainingPlan readyBow = plans.stream().filter(p -> p.method().id.equals("fletching_bows")
                && p.readiness != null && p.readiness.actionable()).findFirst().get();
        assertTrue(readyBow.comparison.reason.contains("more preparation")
                || readyBow.comparison.reason.contains("deterministic catalog order"));
        assertTrue(plans.stream().filter(p -> p.comparison != null)
                .anyMatch(p -> p.comparison.reason.contains("unresolved requirements")));
    }

    @Test
    public void detailedWhyIncludesAlternativeWithoutInternalRecipeIds()
    {
        TrainingPlan plan = plans(MethodIntelligenceTest.data(1, -1,
                inventory(), Collections.emptyList())).stream()
                .filter(p -> p.method().id.equals("fletching_bows") && p.readiness.actionable())
                .findFirst().get().withCurrentStageTargetLevel(85);
        Recommendation recommendation = new Recommendation("skill:fletching", "Train Fletching", "Good fit.",
                20, plan, Confidence.VERIFIED, 80, 85, plan.readiness.guidance(85));
        String details = Presentation.detailedText(recommendation);
        assertTrue(details, details.contains("Compared with"));
        assertFalse(details, details.contains("yew_shortbow_cutting"));
        assertFalse(details, details.contains("Strategist will verify"));
    }

    private static List<TrainingPlan> plans(GameData data)
    {
        MethodReadinessService readiness = new MethodReadinessService(new MethodIntelligenceCatalog(),
                new MethodPreparationService(MethodIntelligenceTest.resources(0, 0)));
        TrainingMethodSelector selector = new TrainingMethodSelector(new TrainingMethodCatalog(), null,
                new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(),
                new UimInventoryResolutionService(), readiness);
        return selector.rankedCandidates(data, Skill.FLETCHING, 80,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false);
    }

    private static List<ItemState> inventory()
    {
        List<ItemState> inventory = MethodIntelligenceTest.carried(1511, "Logs", 4);
        inventory.addAll(MethodIntelligenceTest.carried(1515, "Yew logs", 6));
        inventory.add(new ItemState(946, "Knife", 1));
        return inventory;
    }
}
