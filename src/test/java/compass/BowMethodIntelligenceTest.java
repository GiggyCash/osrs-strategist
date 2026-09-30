package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class BowMethodIntelligenceTest
{
    private final MethodIntelligenceCatalog catalog = new MethodIntelligenceCatalog();
    private final TrainingMethod method = new TrainingMethodCatalog().curatedFor(Skill.FLETCHING).stream()
            .map(CuratedTrainingMethod::method).filter(m -> m.id.equals("fletching_bows"))
            .findFirst().orElseThrow(AssertionError::new);

    @Test
    public void fullCarriedInventoryRetainsKnifeAndAllBowOutputs()
    {
        List<ItemState> carried = logs(27);
        carried.add(new ItemState(946, "Knife", 1));
        MethodReadiness ready = evaluate(MethodIntelligenceTest.data(2, -1, carried, Collections.emptyList()), 0);
        assertTrue(ready.actionable());
        assertEquals(27, ready.batch);
        assertEquals(RequirementState.VERIFIED, ready.capacity.efficient);
        assertFalse(ready.method.bankLoop);
        assertTrue(ready.guidance(85).getNote().contains("unstrung bows"));
        carried.remove(carried.size() - 1);
        carried.add(new ItemState(99999, "Protected setup", 1));
        assertFalse(evaluate(MethodIntelligenceTest.data(2, -1, carried, Collections.emptyList()), 0).actionable());
    }

    @Test
    public void bankRetrievalCannotLeakIntoUimSupplies()
    {
        List<ItemState> bank = logs(4);
        bank.add(new ItemState(946, "Knife", 1));
        assertTrue(evaluate(MethodIntelligenceTest.data(1, -1, Collections.emptyList(), bank), 0).actionable());
        MethodReadiness uim = evaluate(MethodIntelligenceTest.data(2, -1, Collections.emptyList(), bank), 0);
        for (MethodPreparation.Step step : uim.preparation.steps)
            assertNotEquals(MethodPreparation.Kind.RETRIEVE, step.kind);
    }

    @Test
    public void mainPurchaseNeedsAffordableBatchAndIronNeverBuys()
    {
        assertTrue(evaluate(MethodIntelligenceTest.data(0, 500, Collections.emptyList(), Collections.emptyList()), 100).actionable());
        MethodReadiness poor = evaluate(MethodIntelligenceTest.data(0, 499, Collections.emptyList(), Collections.emptyList()), 100);
        for (MethodPreparation.Step step : poor.preparation.steps)
            assertNotEquals(MethodPreparation.Kind.BUY, step.kind);
        MethodReadiness iron = evaluate(MethodIntelligenceTest.data(1, 500, Collections.emptyList(), Collections.emptyList()), 100);
        if (iron.preparation != null)
            for (MethodPreparation.Step step : iron.preparation.steps)
                assertNotEquals(MethodPreparation.Kind.BUY, step.kind);
    }

    @Test
    public void workingAndEfficientCapacityRespectRetainedSetup()
    {
        List<ItemState> setup = MethodIntelligenceTest.carried(99999, "Protected setup", 24);
        MethodCapacity capacity = MethodCapacity.evaluate(new ItemsState(setup, true), catalog.recipe("shortbow_cutting"), 4);
        assertEquals(RequirementState.VERIFIED, capacity.hard);
        assertEquals(RequirementState.BLOCKED, capacity.working);
        assertEquals(RequirementState.BLOCKED, capacity.efficient);
        assertEquals(12, catalog.method("fletching_bows").recipes.size());
    }

    @Test
    public void membershipFailsClosedAndCarriedLogChangesChosenRecipe()
    {
        for (Membership membership : new Membership[] {Membership.F2P, Membership.UNKNOWN})
        {
            Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
            levels.put(Skill.FLETCHING, 80);
            AccountSnapshot account = new AccountSnapshot("test", 1, 0, "MAIN", membership,
                    1, 1, 0, levels, Collections.emptyMap());
            assertFalse(evaluate(GameData.builder(account).build(), 100).actionable());
        }
        List<ItemState> yew = MethodIntelligenceTest.carried(1515, "Yew logs", 4);
        yew.add(new ItemState(946, "Knife", 1));
        MethodReadiness ready = evaluate(MethodIntelligenceTest.data(2, -1, yew, Collections.emptyList()), 0);
        assertTrue(ready.actionable());
        assertEquals("yew_longbow_cutting", ready.recipe.id);
        assertEquals(4, ready.batch);
    }

    @Test
    public void levelBoundariesChangeRecipeWithoutInventingLockedOutputs()
    {
        List<ItemState> carried = logs(4);
        carried.add(new ItemState(946, "Knife", 1));
        for (int level : new int[] {4, 5, 9, 10})
        {
            GameData data = atLevel(level, new ItemsState(carried, true), ItemsState.unknown());
            MethodReadiness ready = evaluate(data, 0);
            if (level < 5) assertFalse(ready.actionable());
            else
            {
                assertTrue(ready.actionable());
                assertEquals(level < 10 ? "shortbow_cutting" : "longbow_cutting", ready.recipe.id);
                assertTrue(ready.recipe.level <= level);
            }
        }
    }

    @Test
    public void consumedLogsRetainedOutputsAndMovedKnifeInvalidateReadyState()
    {
        List<ItemState> carried = logs(4);
        carried.add(new ItemState(946, "Knife", 1));
        assertEquals(MethodPreparation.State.READY,
                evaluate(atLevel(10, new ItemsState(carried, true), ItemsState.unknown()), 0).preparation.state);
        List<ItemState> outputs = MethodIntelligenceTest.carried(48, "Longbow (u)", 27);
        outputs.add(new ItemState(946, "Knife", 1));
        assertFalse(evaluate(atLevel(10, new ItemsState(outputs, true), ItemsState.unknown()), 0).actionable());
        MethodReadiness moved = evaluate(atLevel(10, new ItemsState(logs(4), true),
                new ItemsState(Collections.singletonList(new ItemState(946, "Knife", 1)), true)), 0);
        assertNotEquals(MethodPreparation.State.READY, moved.preparation.state);
        for (MethodPreparation.Step step : moved.preparation.steps)
            assertNotEquals(MethodPreparation.Kind.RETRIEVE, step.kind);
        assertFalse(evaluate(atLevel(10, ItemsState.unknown(), ItemsState.unknown()), 0).actionable());
    }

    private static GameData atLevel(int level, ItemsState inventory, ItemsState bank)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        levels.put(Skill.FLETCHING, level);
        AccountSnapshot account = new AccountSnapshot("test", 1, 2, "ULTIMATE_IRONMAN", Membership.P2P,
                1, level, 0, levels, Collections.emptyMap());
        return GameData.builder(account).inventory(inventory).equipment(new ItemsState(Collections.emptyList(), true))
                .bank(bank).build();
    }

    @Test
    public void sharedGuidancePutsPurchaseAndRetrievalBeforeProcessing()
    {
        MethodReadiness purchase = evaluate(MethodIntelligenceTest.data(0, 500,
                Collections.emptyList(), Collections.emptyList()), 100);
        assertTrue(purchase.guidance(85).getAction().startsWith("First, Buy "));
        assertTrue(purchase.guidance(85).getAction().contains(" Then Use a knife"));
        List<ItemState> stock = logs(4);
        stock.add(new ItemState(946, "Knife", 1));
        MethodReadiness retrieval = evaluate(MethodIntelligenceTest.data(1, -1,
                Collections.emptyList(), stock), 0);
        assertTrue(retrieval.guidance(85).getAction().startsWith("First, Withdraw observed bank stock:"));
        MethodReadiness ready = evaluate(MethodIntelligenceTest.data(1, -1,
                stock, Collections.emptyList()), 0);
        assertTrue(ready.guidance(85).getAction().startsWith("Use a knife"));
        assertFalse(ready.guidance(85).getAction().contains("First,"));
    }

    private MethodReadiness evaluate(GameData data, int price)
    {
        AccountResourcePlanner resources = new AccountResourcePlanner(new MarketPriceService(null)
        {
            @Override public MarketPriceQuote quote(String name)
            {
                int id = name.equals("Logs") ? 1511 : name.equals("Knife") ? 946 : -1;
                return id < 0 ? null : new MarketPriceQuote(id, name, price, System.currentTimeMillis());
            }
        }, new ResourceSourceCatalog());
        return new MethodReadinessService(catalog, new MethodPreparationService(resources)).evaluate(data, method, false);
    }

    private static List<ItemState> logs(int count)
    {
        return MethodIntelligenceTest.carried(1511, "Logs", count);
    }
}
