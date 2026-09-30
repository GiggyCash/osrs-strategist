package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static compass.MethodIntelligenceTest.*;
import static org.junit.Assert.*;

public class MethodIntelligenceSafetyTest
{
    @Test
    public void duplicateRequestsCannotSpendTheSameStockTwice()
    {
        MethodPreparationService service = new MethodPreparationService(resources(100, 0));
        List<MethodInput> needs = Arrays.asList(new MethodInput("Raw salmon", 331, 4),
                new MethodInput("Raw salmon", 331, 4));
        GameData main = data(0, 400, carried(331, "Raw salmon", 4), Collections.emptyList());
        MethodPreparation prepared = service.evaluate(main, needs, false, true, true);
        assertEquals(MethodPreparation.State.VERIFIED, prepared.state);
        assertEquals(400L, prepared.purchaseCost);
        assertEquals(2, prepared.steps.size());
        assertFalse(service.evaluate(data(1, 400, carried(331, "Raw salmon", 4),
                Collections.emptyList()), needs, false, true, true).feasible());
    }

    @Test
    public void largeLineCostIsNotMultipliedAsAnInt()
    {
        List<MethodInput> needs = Collections.singletonList(new MethodInput("Raw salmon", 331, 4));
        GameData poor = data(0, 10_000, Collections.emptyList(), Collections.emptyList());
        AccountResourcePlanner.Purchase purchase = resources(1_100_000_000, 0).purchase(poor, needs);
        assertEquals(4_400_000_000L, purchase.cost);
        assertEquals(RequirementState.BLOCKED, purchase.state);
    }

    @Test
    public void unknownBankCannotProveAPurchaseShortfall()
    {
        GameData unknown = GameData.builder(account(0))
                .inventory(new ItemsState(Collections.emptyList(), true))
                .equipment(new ItemsState(Collections.emptyList(), true))
                .bank(ItemsState.unknown())
                .economy(new AccountEconomySnapshot(10_000, 0, Confidence.VERIFIED)).build();
        assertFalse(new MethodPreparationService(resources(100, 0)).evaluate(unknown,
                Collections.singletonList(new MethodInput("Raw salmon", 331, 4)),
                false, true, true).feasible());
    }

    @Test
    public void staleOrFutureCashCannotFundPreparation()
    {
        for (long offset : new long[] {-600_000, 60_000})
        {
            GameData data = GameData.builder(account(0)).economy(new AccountEconomySnapshot(
                    10_000, 0, Confidence.VERIFIED, System.currentTimeMillis() + offset)).build();
            assertEquals(RequirementState.CHECK_NEEDED, resources(100, 0).purchase(data,
                    Collections.singletonList(new MethodInput("Raw salmon", 331, 4))).state);
        }
    }

    @Test
    public void aSingleCarriedInputIsAUsefulSafeUimBatch()
    {
        MethodReadiness ready = readiness(data(2, -1, carried(331, "Raw salmon", 1),
                Collections.emptyList()));
        assertTrue(ready.actionable());
        assertEquals(1, ready.batch);
    }

    @Test
    public void typedReadinessCannotOverrideAnotherUnknownAccessGate()
    {
        MethodReadiness ready = readiness(data(2, -1, carried(331, "Raw salmon", 8),
                Collections.emptyList()));
        TrainingPlan plan = new TrainingPlan(method(), "", Confidence.VERIFIED,
                Collections.singletonList(new EvidenceCheck("quest:extra", "Required quest",
                        RequirementState.CHECK_NEEDED, "Quest completion is unknown.")))
                .withReadiness(ready);
        Guidance guidance = ready.guidance(85);
        Recommendation recommendation = new Recommendation("skill:cooking", "Train Cooking to 85",
                "Carried supplies", 1_000_000, plan, Confidence.VERIFIED, 80, 85,
                guidance, Safety.skill(true, Skill.COOKING));
        assertFalse(new ActionabilityPolicy().canLeadQueue(recommendation));
        assertFalse(RequirementActionability.isActionablePreparation(plan, guidance));
    }

    @Test
    public void invalidBatchCannotProduceVerifiedCapacity()
    {
        MethodIntelligenceCatalog.Recipe recipe = new MethodIntelligenceCatalog().recipe("salmon");
        for (int batch : new int[] {0, -1, 29, Integer.MAX_VALUE})
            assertEquals(RequirementState.BLOCKED, MethodCapacity.evaluate(
                    new ItemsState(Collections.emptyList(), true), recipe, batch).working);
    }

    @Test
    public void efficientBatchNeedsItsOwnCapacityAndPreparedQuantity()
    {
        MethodIntelligenceCatalog.Recipe recipe = new MethodIntelligenceCatalog().recipe("salmon");
        ItemsState empty = new ItemsState(Collections.emptyList(), true);
        assertEquals(RequirementState.CHECK_NEEDED, MethodCapacity.evaluate(empty, recipe, 4).efficient);
        assertEquals(RequirementState.VERIFIED, MethodCapacity.evaluate(empty, recipe, 28).efficient);
        ItemsState setup = new ItemsState(carried(9000, "Retained setup", 1), true);
        MethodCapacity limited = MethodCapacity.evaluate(setup, recipe, 4);
        assertEquals(RequirementState.VERIFIED, limited.working);
        assertEquals(RequirementState.BLOCKED, limited.efficient);
    }

    @Test
    public void reusableToolRemainsInInventoryUnlessEquippedUseIsExplicit()
    {
        MethodIntelligenceCatalog.Recipe recipe = new MethodIntelligenceCatalog().recipe("salmon");
        MethodIntelligenceCatalog.Ingredient tool = new MethodIntelligenceCatalog.Ingredient();
        tool.itemId = 9001;
        tool.name = "Synthetic reusable tool";
        tool.quantity = 1;
        recipe.tools.add(tool);
        ItemsState full = new ItemsState(carried(331, "Raw salmon", 28), true);
        ItemsState equipment = new ItemsState(carried(9001, tool.name, 1), true);
        assertEquals(RequirementState.BLOCKED, MethodCapacity.evaluate(full, equipment, recipe, 28).working);
        tool.equippedAllowed = true;
        assertEquals(RequirementState.VERIFIED, MethodCapacity.evaluate(full, equipment, recipe, 28).working);
        assertEquals(RequirementState.BLOCKED, MethodCapacity.evaluate(full, null, recipe, 28).working);
        List<ItemState> inventory = carried(331, "Raw salmon", 27);
        inventory.addAll(carried(9001, tool.name, 1));
        assertEquals(RequirementState.VERIFIED,
                MethodCapacity.evaluate(new ItemsState(inventory, true), null, recipe, 27).working);
    }

    @Test
    public void equippedReusablePreparationDoesNotTellPlayerToUnequipIt()
    {
        GameData data = GameData.builder(account(2))
                .inventory(new ItemsState(Collections.emptyList(), true))
                .equipment(new ItemsState(carried(9001, "Synthetic tool", 1), true)).build();
        MethodPreparation prep = new MethodPreparationService(resources(0, 0)).evaluate(data,
                Collections.singletonList(new MethodInput("Synthetic tool", 9001, 1)),
                false, false, false, Collections.singleton(9001));
        assertEquals(MethodPreparation.State.READY, prep.state);
        assertTrue(prep.explanation().contains("equipped reusable tool"));
        assertFalse(prep.explanation().contains("Unequip"));
    }

    private static MethodReadiness readiness(GameData data)
    {
        return new MethodReadinessService(new MethodIntelligenceCatalog(),
                new MethodPreparationService(resources(0, 0))).evaluate(data, method(), false);
    }

    private static TrainingMethod method()
    {
        return new TrainingMethodCatalog().f2pFor(Skill.COOKING).stream()
                .map(CuratedTrainingMethod::method)
                .filter(method -> method.id.equals("cooking_f2p_uim_carried_fish"))
                .findFirst().orElseThrow(AssertionError::new);
    }
}
