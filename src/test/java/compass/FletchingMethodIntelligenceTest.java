package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class FletchingMethodIntelligenceTest
{
    private final MethodIntelligenceCatalog catalog = new MethodIntelligenceCatalog();
    private final MethodReadinessService readiness = new MethodReadinessService(catalog,
            new MethodPreparationService(new AccountResourcePlanner(null, new ResourceSourceCatalog())));

    @Test
    public void fullCarriedBatchRetainsKnifeAndCreatesOnlyOneOutputStack()
    {
        MethodReadiness result = evaluate(data(2, Membership.P2P, 1, logsWithKnife(1511, 27), empty()));
        assertTrue(result.actionable());
        assertEquals(27, result.batch);
        assertEquals(MethodPreparation.State.READY, result.preparation.state);
        assertEquals(RequirementState.VERIFIED, result.capacity.efficient);
        assertEquals(15, result.recipe.outputsPerAction.intValue());
        assertTrue(result.guidance(30).getAction().contains("Stop at level 15"));
        assertTrue(result.guidance(30).getAction().contains("arrow shafts"));
    }

    @Test
    public void optionalBankPreparationRequiresOneReusableKnife()
    {
        List<ItemState> bank = Arrays.asList(new ItemState(1511, "Logs", 100), new ItemState(946, "Knife", 1));
        MethodReadiness result = evaluate(data(1, Membership.P2P, 1, empty(), bank));
        assertTrue(result.actionable());
        assertEquals(MethodPreparation.State.VERIFIED, result.preparation.state);
        assertTrue(result.preparation.explanation().contains("4 Logs"));
        assertTrue(result.preparation.explanation().contains("1 Knife"));
        assertFalse(result.preparation.explanation().contains("4 Knife"));
        assertEquals(BankingMode.LOCAL_PROCESSING, result.guidance(15).getBankingBehavior());
    }

    @Test
    public void uimCannotRetrieveTheSameSuppliesFromAConventionalBank()
    {
        List<ItemState> stock = Arrays.asList(new ItemState(1511, "Logs", 100), new ItemState(946, "Knife", 1));
        for (List<ItemState> carried : Arrays.asList(empty(), MethodIntelligenceTest.carried(1511, "Logs", 4)))
        {
            MethodReadiness result = evaluate(data(2, Membership.P2P, 1, carried, stock));
            assertTrue(result.actionable());
            assertEquals(MethodPreparation.State.VERIFIED, result.preparation.state);
            assertFalse(result.preparation.steps.stream().anyMatch(step -> step.kind == MethodPreparation.Kind.RETRIEVE));
            assertTrue(result.preparation.steps.stream().anyMatch(step -> step.kind == MethodPreparation.Kind.ACQUIRE));
            assertTrue(result.guidance(15).getAction().startsWith("First, Collect"));
        }
    }

    @Test
    public void freeAndUnknownMembershipCannotUseTheMethodEvenWithAllItems()
    {
        for (Membership membership : new Membership[] {Membership.F2P, Membership.UNKNOWN})
            assertEquals(RequirementState.BLOCKED,
                    evaluate(data(0, membership, 1, logsWithKnife(1511, 4), empty())).access);
    }

    @Test
    public void oakUnlockChangesRecipeWithoutAssumingFutureLevels()
    {
        List<ItemState> stock = logsWithKnife(1521, 4);
        MethodReadiness lockedOak = evaluate(data(1, Membership.P2P, 14, stock, empty()));
        assertTrue(lockedOak.actionable());
        assertEquals("regular_arrow_shafts", lockedOak.recipe.id);
        assertTrue(lockedOak.preparation.steps.stream().anyMatch(step -> step.kind == MethodPreparation.Kind.ACQUIRE));
        MethodReadiness result = evaluate(data(1, Membership.P2P, 15, stock, empty()));
        assertTrue(result.actionable());
        assertEquals("oak_arrow_shafts", result.recipe.id);
        assertEquals(30, result.recipe.outputsPerAction.intValue());
        assertTrue(result.guidance(80).getAction().contains("Stop at level 21"));
    }

    @Test
    public void existingStackAndRepresentableOutputLimitAreRespected()
    {
        List<ItemState> items = logsWithKnife(1511, 26);
        items.add(new ItemState(52, "Arrow shaft", 10));
        MethodReadiness result = evaluate(data(2, Membership.P2P, 1, items, empty()));
        assertTrue(result.actionable());
        assertEquals(26, result.batch);
        assertEquals(RequirementState.BLOCKED, result.capacity.efficient);
        items.set(items.size() - 1, new ItemState(52, "Arrow shaft", Integer.MAX_VALUE - 14));
        assertEquals(RequirementState.BLOCKED,
                MethodCapacity.evaluate(new ItemsState(items, true), catalog.recipe("regular_arrow_shafts"), 1).hard);
    }

    @Test
    public void productionSelectorAndFinalGateShareTheEvaluatedRecipe()
    {
        GameData data = data(2, Membership.P2P, 1, logsWithKnife(1511, 4), empty());
        TrainingMethodSelector selector = new TrainingMethodSelector(new TrainingMethodCatalog(),
                null, new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(),
                new UimInventoryResolutionService(), readiness);
        TrainingPlan plan = selector.select(data, Skill.FLETCHING, 1,
                StrategyMode.BALANCED, SessionIntent.QUICK_20_MIN);
        assertNotNull(plan);
        assertEquals("fletching_arrow_shafts", plan.method().id);
        assertTrue(plan.readiness.actionable());
        Recommendation recommendation = new Recommendation("skill:fletching", "Train Fletching to 15",
                "Prepared arrow shafts", 100, plan, plan.confidence, 1, 15,
                plan.readiness.guidance(15), Safety.skill(true, Skill.FLETCHING));
        assertTrue(new ActionabilityPolicy().canLeadQueue(recommendation));
    }

    @Test
    public void pickupPreparationCannotOverrideAFullProtectedInventory()
    {
        List<ItemState> items = MethodIntelligenceTest.carried(9001, "Retained setup", 28);
        MethodReadiness result = evaluate(data(2, Membership.P2P, 1, items, empty()));
        assertEquals(MethodPreparation.State.VERIFIED, result.preparation.state);
        assertTrue(result.preparation.steps.stream().anyMatch(step -> step.kind == MethodPreparation.Kind.ACQUIRE));
        assertEquals(RequirementState.BLOCKED, result.capacity.hard);
        assertFalse(result.actionable());
        assertNull(result.guidance(15));
    }

    private MethodReadiness evaluate(GameData data)
    {
        TrainingMethod method = new TrainingMethodCatalog().curatedFor(Skill.FLETCHING).stream()
                .map(CuratedTrainingMethod::method).filter(value -> value.id.equals("fletching_arrow_shafts"))
                .findFirst().orElseThrow(AssertionError::new);
        return readiness.evaluate(data, method, false);
    }

    private static GameData data(int mode, Membership membership, int level,
            List<ItemState> inventory, List<ItemState> bank)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) levels.put(skill, 1);
        levels.put(Skill.FLETCHING, level);
        AccountSnapshot account = new AccountSnapshot("fixture", 0, mode,
                AccountMode.fromTypeCode(mode).name(), membership, 0, 0, 0, levels, Collections.emptyMap());
        return GameData.builder(account).inventory(new ItemsState(inventory, true))
                .equipment(new ItemsState(empty(), true)).bank(new ItemsState(bank, System.currentTimeMillis())).build();
    }

    private static List<ItemState> logsWithKnife(int id, int count)
    {
        List<ItemState> items = MethodIntelligenceTest.carried(id, id == 1511 ? "Logs" : "Oak logs", count);
        items.add(new ItemState(946, "Knife", 1));
        return items;
    }

    private static List<ItemState> empty() { return Collections.emptyList(); }
}
