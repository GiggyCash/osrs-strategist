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
