package compass;

import com.google.gson.Gson;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecipeEconomicsTest
{
    private static final String SHORT = "shortbow_cutting", LONG = "longbow_cutting";
    private static final String YEW = "yew_longbow_cutting";

    @Test
    public void existingValueWeightsCompareCostsAndReturnsWithoutCheapestInputRule()
    {
        MethodIntelligenceCatalog.Bundle bundle = bundle();
        fill(bundle, value(0, 1));
        recipe(bundle, SHORT).economics.put(AccountMode.IRONMAN, value(0, 0));
        recipe(bundle, LONG).economics.put(AccountMode.IRONMAN, value(1, 0.5));
        // More burden can be worthwhile: existing resourceFit * 8 - opportunityCost * 12 = 2.
        List<TrainingPlan> plans = plans(bundle, data(1), StrategyMode.BALANCED, SessionIntent.ONE_HOUR);
        assertEquals(LONG, plans.get(0).readiness.recipe.id);
        TrainingMethodPolicy policy = new TrainingMethodPolicy();
        assertEquals(2, policy.economicScore(plans.get(0).readiness.economics), 0);
        assertEquals(0, policy.economicScore(find(plans, SHORT).readiness.economics), 0);
        assertEquals(-12, policy.economicScore(find(plans, YEW).readiness.economics), 0);
        assertEquals(CandidateComparison.Basis.ECONOMICS, plans.get(0).comparison.basis);
        assertTrue(plans.get(0).comparison.reason.contains("reviewed resource value"));
        // Reordering source references cannot change a distinct economic winner.
        Collections.reverse(bundle.methods.stream().filter(m -> m.ids.contains("fletching_bows"))
                .findFirst().get().recipes);
        assertEquals(LONG, plans(bundle, data(1), StrategyMode.BALANCED, SessionIntent.ONE_HOUR)
                .get(0).readiness.recipe.id);
    }

    @Test
    public void equalEconomicsPreservesEveryExistingRecipeOrderAcrossStrategyAndSession()
    {
        for (StrategyMode mode : StrategyMode.values())
            for (SessionIntent intent : SessionIntent.values())
            {
                List<String> baseline = ids(plans(bundle(), data(1), mode, intent));
                MethodIntelligenceCatalog.Bundle equal = bundle();
                fill(equal, value(0.75, 0.5)); // Different components, net zero under existing weights.
                recipe(equal, YEW).economics.put(AccountMode.IRONMAN, value(0, 0));
                assertEquals(baseline, ids(plans(equal, data(1), mode, intent)));
                assertEquals(YEW, baseline.get(0));
            }
    }

    @Test
    public void missingEconomicsIsUnknownNotAFreeRecipeOrFeasibilityFailure()
    {
        MethodIntelligenceCatalog.Bundle mixed = bundle();
        fill(mixed, value(0, 1));
        recipe(mixed, SHORT).economics.clear();
        List<TrainingPlan> plans = plans(mixed, data(1), StrategyMode.BALANCED, SessionIntent.ONE_HOUR);
        assertEquals(ids(plans(bundle(), data(1), StrategyMode.BALANCED, SessionIntent.ONE_HOUR)), ids(plans));
        assertNull(find(plans, SHORT).readiness.economics);
        assertTrue(find(plans, SHORT).readiness.actionable());
        assertNotNull(find(plans, YEW).readiness.economics);
        assertFalse(plans.get(0).getWhyThisMethod().contains("reviewed resource benefits"));
    }

    @Test
    public void economicsCannotOverridePreparationOrMakeAnUnusableRecipeReady()
    {
        MethodIntelligenceCatalog.Bundle bundle = bundle();
        fill(bundle, value(-1, 1));
        recipe(bundle, YEW).economics.put(AccountMode.IRONMAN, value(1, 0));
        List<ItemState> logs = MethodIntelligenceTest.carried(1511, "Logs", 4);
        logs.add(new ItemState(946, "Knife", 1));
        GameData banked = MethodIntelligenceTest.data(1, -1, logs,
                MethodIntelligenceTest.carried(1515, "Yew logs", 6));
        List<TrainingPlan> plans = plans(bundle, banked, StrategyMode.BALANCED, SessionIntent.ONE_HOUR);
        assertEquals(LONG, plans.get(0).readiness.recipe.id);
        assertEquals(MethodPreparation.State.VERIFIED, find(plans, YEW).readiness.preparation.state);
        GameData absent = MethodIntelligenceTest.data(1, -1, logs, Collections.emptyList());
        assertTrue(plans(bundle(), absent, StrategyMode.BALANCED, SessionIntent.ONE_HOUR).stream()
                .noneMatch(p -> p.readiness.recipe.id.equals(YEW)));
        assertEquals(LONG, plans(bundle, absent, StrategyMode.BALANCED, SessionIntent.ONE_HOUR)
                .get(0).readiness.recipe.id);
    }

    @Test
    public void selectedEconomicsStaysWithItsOwnPreparationCapacityAndGuidance()
    {
        MethodIntelligenceCatalog.Bundle bundle = bundle();
        fill(bundle, value(0, 1));
        recipe(bundle, SHORT).economics.put(AccountMode.IRONMAN, value(0, 0));
        List<TrainingPlan> plans = plans(bundle, data(1), StrategyMode.EFFICIENT, SessionIntent.QUICK_20_MIN);
        TrainingPlan selected = plans.get(0);
        assertEquals(SHORT, selected.readiness.recipe.id);
        assertEquals(4, selected.readiness.batch);
        assertEquals(4, selected.readiness.capacity.actions);
        assertTrue(selected.readiness.preparation.steps.stream().anyMatch(s -> s.input.itemId == 1511
                && s.input.quantity == 4 && s.kind == MethodPreparation.Kind.CARRIED));
        assertSame(selected.readiness, selected.withCurrentStageTargetLevel(85).readiness);
        assertTrue(selected.readiness.guidance(85).getAction().contains(selected.readiness.recipe.action));
        assertEquals(6, find(plans, YEW).readiness.batch);
        // Local tie-break is not charged a second time through rendered final-queue value.
        assertEquals(0, selected.readiness.guidance(85).getStrategicValue().scoreDelta(), 0);
    }

    @Test
    public void accountModesRequireTheirOwnEvidenceAndProductionRemainsUnrated()
    {
        MethodIntelligenceCatalog.Bundle bundle = bundle();
        fill(bundle, value(0, 1));
        recipe(bundle, SHORT).economics.put(AccountMode.IRONMAN, value(1, 0));
        for (int code : new int[] {0, 3, 4, 5, 6})
        {
            List<TrainingPlan> plans = plans(bundle, data(code), StrategyMode.BALANCED, SessionIntent.ONE_HOUR);
            assertEquals(YEW, plans.get(0).readiness.recipe.id);
            assertTrue(plans.stream().allMatch(p -> p.readiness.economics == null));
        }
        assertEquals(YEW, plans(bundle, data(2), StrategyMode.BALANCED, SessionIntent.ONE_HOUR)
                .get(0).readiness.recipe.id);
        MethodIntelligenceCatalog production = new MethodIntelligenceCatalog();
        for (String method : production.methodIds())
            for (String id : production.method(method).recipes) assertTrue(production.recipe(id).economics.isEmpty());
        assertNull(production.method("crafting_gems"));
    }

    @Test
    public void malformedOrUnattributedEconomicsCannotEnterRanking()
    {
        rejects(b -> recipe(b, SHORT).economics = null);
        rejects(b -> recipe(b, SHORT).economics.put(AccountMode.UNKNOWN, value(0, 0)));
        rejects(b -> recipe(b, SHORT).economics.put(AccountMode.IRONMAN, null));
        rejects(b -> recipe(b, SHORT).economics.put(AccountMode.IRONMAN, new MethodIntelligenceCatalog.Economics()));
        for (String json : Arrays.asList(
                "{\"resourceFit\":2,\"evidence\":[\"review\"]}",
                "{\"opportunityCost\":-1,\"evidence\":[\"review\"]}",
                "{\"resourceFit\":NaN,\"evidence\":[\"review\"]}",
                "{\"resourceFit\":Infinity,\"evidence\":[\"review\"]}",
                "{\"evidence\":[\" \"]}", "{}"))
            rejects(b -> recipe(b, SHORT).economics.put(AccountMode.IRONMAN,
                    new Gson().fromJson(json, MethodIntelligenceCatalog.Economics.class)));
    }

    private static MethodIntelligenceCatalog.Economics value(double returns, double cost)
    {
        MethodIntelligenceCatalog.Economics value = new MethodIntelligenceCatalog.Economics();
        value.resourceFit = returns;
        value.opportunityCost = cost;
        value.evidence = Collections.singletonList("synthetic-reviewed-economics");
        return value;
    }

    private static MethodIntelligenceCatalog.Bundle bundle()
    {
        return BundledCatalogLoader.array("/content/catalogs/method-intelligence.json",
                MethodIntelligenceCatalog.Bundle[].class)[0];
    }

    private static MethodIntelligenceCatalog.Recipe recipe(MethodIntelligenceCatalog.Bundle bundle, String id)
    {
        return bundle.recipes.stream().filter(r -> r.id.equals(id)).findFirst().get();
    }

    private static void fill(MethodIntelligenceCatalog.Bundle bundle, MethodIntelligenceCatalog.Economics value)
    {
        bundle.recipes.forEach(r -> r.economics.put(AccountMode.IRONMAN, value));
    }

    private static GameData data(int mode)
    {
        List<ItemState> inventory = MethodIntelligenceTest.carried(1511, "Logs", 4);
        inventory.addAll(MethodIntelligenceTest.carried(1515, "Yew logs", 6));
        inventory.add(new ItemState(946, "Knife", 1));
        return MethodIntelligenceTest.data(mode, -1, inventory, Collections.emptyList());
    }

    private static List<TrainingPlan> plans(MethodIntelligenceCatalog.Bundle bundle, GameData data,
            StrategyMode mode, SessionIntent intent)
    {
        Gson gson = new Gson();
        MethodIntelligenceCatalog.Bundle copy = gson.fromJson(gson.toJson(bundle), MethodIntelligenceCatalog.Bundle.class);
        MethodReadinessService readiness = new MethodReadinessService(new MethodIntelligenceCatalog(copy),
                new MethodPreparationService(MethodIntelligenceTest.resources(0, 0)));
        TrainingMethodSelector selector = new TrainingMethodSelector(new TrainingMethodCatalog(), null,
                new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(),
                new UimInventoryResolutionService(), readiness);
        return selector.rankedCandidates(data, Skill.FLETCHING, 80, mode, intent, false, false).stream()
                .filter(p -> p.method().id.equals("fletching_bows") && p.readiness.actionable())
                .collect(Collectors.toList());
    }

    private static TrainingPlan find(List<TrainingPlan> plans, String recipe)
    { return plans.stream().filter(p -> p.readiness.recipe.id.equals(recipe)).findFirst().get(); }

    private static List<String> ids(List<TrainingPlan> plans)
    { return plans.stream().map(p -> p.readiness.recipe.id).collect(Collectors.toList()); }

    private static void rejects(Consumer<MethodIntelligenceCatalog.Bundle> mutation)
    {
        MethodIntelligenceCatalog.Bundle bundle = bundle();
        mutation.accept(bundle);
        assertThrows(IllegalStateException.class, () -> new MethodIntelligenceCatalog(bundle));
    }
}
