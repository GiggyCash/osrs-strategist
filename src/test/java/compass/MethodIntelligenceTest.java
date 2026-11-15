package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class MethodIntelligenceTest
{
    private final MethodIntelligenceCatalog catalog = new MethodIntelligenceCatalog();

    @Test
    public void purchaseRequiresExactQuantityUsablePricesAndCash()
    {
        List<MethodInput> needs = Collections.singletonList(new MethodInput("Raw salmon", 331, 20));
        assertEquals(RequirementState.VERIFIED, resources(100, 0).purchase(data(0, 2_000, empty(), empty()), needs).state);
        assertEquals(RequirementState.BLOCKED, resources(100, 0).purchase(data(0, 1_999, empty(), empty()), needs).state);
        assertEquals(RequirementState.CHECK_NEEDED, resources(100, 0).purchase(data(0, -1, empty(), empty()), needs).state);
        assertEquals(RequirementState.CHECK_NEEDED, resources(0, 0).purchase(data(0, 9_000, empty(), empty()), needs).state);
        assertEquals(RequirementState.CHECK_NEEDED, resources(100, -600_000).purchase(data(0, 9_000, empty(), empty()), needs).state);
        assertEquals(RequirementState.CHECK_NEEDED, resources(100, 60_000).purchase(data(0, 9_000, empty(), empty()), needs).state);
        assertEquals(RequirementState.BLOCKED, resources(100, 0).purchase(data(1, 9_000, empty(), empty()), needs).state);
    }

    @Test
    public void carriedInputsNeedNoPriceOrAcquisition()
    {
        MethodReadiness ready = evaluate(data(2, -1, carried(331, "Raw salmon", 8), empty()),
                "cooking_f2p_uim_carried_fish", resources(0, 0));
        assertTrue(ready.actionable());
        assertEquals(MethodPreparation.State.READY, ready.preparation.state);
        assertEquals(8, ready.batch);
        assertTrue(ready.guidance(85).supplies.contains("already carried"));
        assertTrue(ready.guidance(85).getAction().contains("review supplies"));
        assertEquals(RequirementState.CHECK_NEEDED, ready.capacity.efficient);
    }

    @Test
    public void supplyProseAndUnknownSourcesCannotLead()
    {
        MethodReadiness unresolved = evaluate(data(1, -1, empty(), empty()), "cooking_wines", resources(100, 0));
        assertFalse(unresolved.actionable());
        assertEquals(MethodPreparation.State.UNRESOLVED, unresolved.preparation.state);
        assertNull(unresolved.guidance(85));
        TrainingMethod method = method("cooking_wines");
        TrainingPlan descriptive = new TrainingPlan(method, "", Confidence.VERIFIED, emptyChecks());
        Recommendation recommendation = new Recommendation("skill:cooking", "Train Cooking to 85", "Supplies",
                1_000_000, descriptive, Confidence.VERIFIED, 80, 85,
                new Guidance("Make wine.", "Get grapes and water.", "Grand Exchange", ""), Safety.skill(true, Skill.COOKING));
        assertFalse(new ActionabilityPolicy().canLeadQueue(recommendation));
    }

    @Test
    public void affordablePreparationDiffersFromReadyAndFromUnknown()
    {
        MethodReadiness affordable = evaluate(data(0, 10_000, empty(), empty()), "cooking_wines", resources(100, 0));
        assertTrue(affordable.actionable());
        assertEquals(MethodPreparation.State.VERIFIED, affordable.preparation.state);
        assertEquals(800, affordable.preparation.purchaseCost);
        for (long cash : new long[]{-1, 0, 799})
            assertFalse(evaluate(data(0, cash, empty(), empty()), "cooking_wines", resources(100, 0)).actionable());
        assertFalse(evaluate(data(0, 10_000, empty(), empty()), "cooking_wines", resources(0, 0)).actionable());
        assertFalse(evaluate(data(0, 10_000, empty(), empty()), "cooking_wines", resources(100, -600_000)).actionable());
    }

    @Test
    public void fullInventoryCanProcessConsumedInputsButCannotInventOutputSpace()
    {
        ItemsState fullFish = new ItemsState(carried(331, "Raw salmon", 28), true);
        assertEquals(RequirementState.VERIFIED, MethodCapacity.evaluate(fullFish, catalog.recipe("salmon"), 28).working);
        MethodIntelligenceCatalog.Recipe expanding = catalog.recipe("salmon");
        expanding.outputsPerAction = 2;
        assertEquals(RequirementState.BLOCKED, MethodCapacity.evaluate(fullFish, expanding, 4).working);
        MethodIntelligenceCatalog.Recipe stacked = catalog.recipe("wine");
        stacked.inputs = Collections.singletonList(stacked.inputs.get(0));
        stacked.inputs.get(0).stackable = true;
        List<ItemState> packed = carried(9000, "Protected setup", 27);
        packed.add(new ItemState(1987, "Synthetic stackable input", 100));
        assertEquals(RequirementState.BLOCKED,
                MethodCapacity.evaluate(new ItemsState(packed, true), stacked, 4).hard);
    }

    @Test
    public void hardWorkingAndEfficientCapacityStayDistinct()
    {
        MethodCapacity capacity = MethodCapacity.evaluate(new ItemsState(carried(9000, "Protected setup", 27), true),
                catalog.recipe("salmon"), 4);
        assertEquals(RequirementState.VERIFIED, capacity.hard);
        assertEquals(RequirementState.BLOCKED, capacity.working);
        assertEquals(RequirementState.BLOCKED, capacity.efficient);
        assertEquals(RequirementState.CHECK_NEEDED,
                MethodCapacity.evaluate(ItemsState.unknown(), catalog.recipe("salmon"), 4).hard);
        List<ItemState> wine = carried(1987, "Grapes", 14);
        wine.addAll(carried(1937, "Jug of water", 14));
        assertEquals(RequirementState.VERIFIED,
                MethodCapacity.evaluate(new ItemsState(wine, true), catalog.recipe("wine"), 14).working);
    }

    @Test
    public void uimCannotBorrowBankOrOrdinaryRetrieval()
    {
        List<ItemState> stock = Collections.singletonList(new ItemState(331, "Raw salmon", 100));
        assertTrue(evaluate(data(1, -1, empty(), stock), "cooking_f2p_fish", resources(0, 0)).actionable());
        assertFalse(evaluate(data(2, -1, empty(), stock), "cooking_f2p_fish", resources(0, 0)).actionable());
        assertFalse(evaluate(data(2, -1, empty(), stock), "cooking_f2p_uim_carried_fish", resources(0, 0)).actionable());
    }

    @Test
    public void groupStockMustBeEnabledExactAndFresh()
    {
        List<MethodInput> needs = Collections.singletonList(new MethodInput("Raw salmon", 331, 4));
        MethodPreparationService service = new MethodPreparationService(resources(0, 0));
        for (long offset : new long[]{0, -600_000, 60_000})
        {
            GameData data = GameData.builder(account(4)).inventory(new ItemsState(empty(), true))
                    .equipment(new ItemsState(empty(), true))
                    .groupStorage(new ItemsState(true, Collections.singletonList(new ItemState(331, "Raw salmon", 4)),
                            System.currentTimeMillis() + offset)).build();
            assertEquals(offset == 0, service.evaluate(data, needs, true, true, false).feasible());
            assertFalse(service.evaluate(data, needs, false, true, false).feasible());
        }
    }

    @Test
    public void sameLevelChangesWinnerWithInputsCashModeAndAttention()
    {
        AccountResourcePlanner resources = resources(100, 0);
        TrainingMethodSelector selector = selector(resources);
        assertEquals("cooking_wines", winner(selector, data(0, 10_000, empty(), empty()), StrategyMode.EFFICIENT, SessionIntent.ONE_HOUR));
        assertEquals("cooking_f2p_fish_baseline", winner(selector, data(0, 10_000, carried(331, "Raw salmon", 8), empty()), StrategyMode.EFFICIENT, SessionIntent.ONE_HOUR));
        // The relaxed curated fish route shares the verified recipe contract with the baseline.
        assertEquals("cooking_f2p_fish", winner(selector, data(0, 10_000, empty(), empty()), StrategyMode.RELAXED, SessionIntent.AFK));
        assertEquals("cooking_f2p_uim_carried_fish", winner(selector, data(2, -1, carried(331, "Raw salmon", 8), empty()), StrategyMode.EFFICIENT, SessionIntent.ONE_HOUR));
        TrainingPlan unknown = selector.select(data(0, -1, empty(), empty()), Skill.COOKING, 80,
                StrategyMode.EFFICIENT, SessionIntent.ONE_HOUR);
        assertTrue(unknown == null || !unknown.readiness.actionable());
    }

    @Test
    public void allProductionCookingIdsHaveContractsButUnknownActivitiesStayUnknown()
    {
        TrainingMethodCatalog production = new TrainingMethodCatalog();
        for (CuratedTrainingMethod candidate : production.curatedFor(Skill.COOKING))
            assertNotNull(candidate.method().id, catalog.method(candidate.method().id));
        for (CuratedTrainingMethod candidate : production.f2pFor(Skill.COOKING))
            assertNotNull(candidate.method().id, catalog.method(candidate.method().id));
        GameData readyStock = data(0, 100_000, empty(), empty());
        assertFalse(evaluate(readyStock, "cooking_karambwan_1t", resources(100, 0)).actionable());
        assertFalse(evaluate(readyStock, "cooking_gnome_restaurant", resources(100, 0)).actionable());
        assertFalse(evaluate(readyStock, "cooking_hosidius", resources(100, 0)).actionable());
    }

    private MethodReadiness evaluate(GameData data, String id, AccountResourcePlanner resources)
    { return new MethodReadinessService(catalog, new MethodPreparationService(resources)).evaluate(data, method(id), false); }

    private TrainingMethodSelector selector(AccountResourcePlanner resources)
    {
        return new TrainingMethodSelector(new TrainingMethodCatalog(),
                new RequirementEvidenceEngine(new FarmingAccessEvaluator(new FarmingAccessCatalog()),
                        new AgilityAccessEvaluator(new AgilityCourseCatalog()), new FarmingSupplyCatalog(), new RunecraftSupplyCatalog()),
                new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(), new UimInventoryResolutionService(),
                new MethodReadinessService(catalog, new MethodPreparationService(resources)));
    }

    private static String winner(TrainingMethodSelector selector, GameData data, StrategyMode style, SessionIntent intent)
    {
        TrainingPlan plan = selector.select(data, Skill.COOKING, 80, style, intent);
        assertNotNull(plan);
        assertTrue(plan.getWhyThisMethod(), plan.readiness.actionable());
        return plan.method().id;
    }

    private static TrainingMethod method(String id)
    {
        TrainingMethodCatalog all = new TrainingMethodCatalog();
        List<CuratedTrainingMethod> methods = new ArrayList<>(all.curatedFor(Skill.COOKING));
        methods.addAll(all.f2pFor(Skill.COOKING));
        for (CuratedTrainingMethod candidate : methods) if (id.equals(candidate.method().id)) return candidate.method();
        throw new AssertionError(id);
    }

    static AccountResourcePlanner resources(int price, long ageOffset)
    {
        return new AccountResourcePlanner(new MarketPriceService(null)
        {
            @Override public MarketPriceQuote quote(String name)
            {
                MethodIntelligenceCatalog catalog = new MethodIntelligenceCatalog();
                for (String id : Arrays.asList("shrimp", "sardine", "herring", "trout", "pike", "salmon", "tuna", "lobster", "swordfish", "wine"))
                    for (MethodIntelligenceCatalog.Ingredient input : catalog.recipe(id).inputs)
                        if (name.equals(input.name)) return new MarketPriceQuote(input.itemId, name, price,
                                System.currentTimeMillis() + ageOffset);
                return null;
            }
        }, new ResourceSourceCatalog());
    }

    static GameData data(int mode, long cash, List<ItemState> carried, List<ItemState> bank)
    {
        return GameData.builder(account(mode)).inventory(new ItemsState(carried, true))
                .equipment(new ItemsState(empty(), true)).bank(new ItemsState(bank, System.currentTimeMillis()))
                .economy(cash < 0 ? null : new AccountEconomySnapshot(cash, 0, Confidence.VERIFIED)).build();
    }

    static AccountSnapshot account(int mode)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) levels.put(skill, 80);
        return new AccountSnapshot("test", 1, mode, AccountMode.fromTypeCode(mode).name(), Membership.P2P,
                1, 1_920, 0, levels, Collections.emptyMap());
    }

    static List<ItemState> carried(int id, String name, int count)
    {
        List<ItemState> items = new ArrayList<>();
        for (int i = 0; i < count; i++) items.add(new ItemState(id, name, 1));
        return items;
    }

    private static List<ItemState> empty() { return Collections.emptyList(); }
    private static List<EvidenceCheck> emptyChecks() { return Collections.emptyList(); }
}
