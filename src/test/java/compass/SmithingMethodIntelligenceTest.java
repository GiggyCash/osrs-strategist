package compass;

import java.util.*;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class SmithingMethodIntelligenceTest
{
    private final MethodIntelligenceCatalog catalog = new MethodIntelligenceCatalog();
    private final TrainingMethod method = new TrainingMethodCatalog().curatedFor(Skill.SMITHING).stream()
            .map(CuratedTrainingMethod::method).filter(m -> m.id.equals("smithing_f2p_platebodies"))
            .findFirst().orElseThrow(AssertionError::new);

    @Test
    public void reviewedRecipesMatchCurrentRuneLiteLevelsAndOutputs()
    {
        Map<String, ActionDef> actions = new RuneLiteSkillActionCatalog().actionsFor(Skill.SMITHING).stream()
                .collect(Collectors.toMap(a -> a.name.toLowerCase(Locale.ROOT), a -> a));
        String[] metals = {"bronze", "iron", "steel", "mithril", "adamant", "rune"};
        int[] levels = {18, 33, 48, 68, 88, 99};
        int[] outputs = {1117, 1115, 1119, 1121, 1123, 1127};
        MethodIntelligenceCatalog.Method contract = catalog.method(method.id);
        assertEquals(6, contract.recipes.size());
        assertEquals(Long.valueOf(15260354), Long.valueOf(contract.sourceRevision));
        for (int i = 0; i < metals.length; i++)
        {
            MethodIntelligenceCatalog.Recipe recipe = catalog.recipe(metals[i] + "_platebody_smithing");
            ActionDef action = actions.get(metals[i] + " platebody");
            assertNotNull(metals[i], action);
            assertEquals(action.level, recipe.level);
            assertEquals(action.itemId, recipe.outputItemId);
            assertEquals(5, recipe.inputs.get(0).quantity);
            assertEquals(2347, recipe.tools.get(0).itemId);
            assertEquals(1, recipe.outputsPerAction.intValue());
        }
    }

    @Test
    public void carriedBarsRetainCandidateSpecificBatchCapacityAndGuidance()
    {
        List<ItemState> inventory = bars(2353, "Steel bar", 25);
        inventory.add(new ItemState(2347, "Hammer", 1));
        MethodReadiness ready = evaluate(MethodIntelligenceTest.data(1, -1,
                inventory, Collections.emptyList()), prices(Collections.emptyMap()));
        assertTrue(ready.actionable());
        assertEquals("steel_platebody_smithing", ready.recipe.id);
        assertEquals(5, ready.batch);
        assertEquals(RequirementState.VERIFIED, ready.capacity.efficient);
        assertEquals(MethodPreparation.State.READY, ready.preparation.state);
        assertTrue(ready.guidance(90).getAction().contains("Steel platebody"));
        assertTrue(ready.guidance(90).getNote().contains("platebodies"));
    }

    @Test
    public void bankPurchaseAndModeBoundariesRemainDistinct()
    {
        List<ItemState> bank = bars(2359, "Mithril bar", 25);
        bank.add(new ItemState(2347, "Hammer", 1));
        MethodReadiness iron = evaluate(MethodIntelligenceTest.data(1, -1,
                Collections.emptyList(), bank), prices(Collections.emptyMap()));
        assertTrue(iron.actionable());
        assertEquals("mithril_platebody_smithing", iron.recipe.id);
        assertTrue(iron.preparation.steps.stream().anyMatch(s -> s.kind == MethodPreparation.Kind.RETRIEVE));

        Map<String, Integer> prices = new HashMap<>();
        prices.put("Steel bar", 10);
        prices.put("Hammer", 100);
        MethodReadiness main = evaluate(MethodIntelligenceTest.data(0, 350,
                Collections.emptyList(), Collections.emptyList()), prices(prices));
        assertTrue(main.actionable());
        assertEquals("steel_platebody_smithing", main.recipe.id);
        assertEquals(350, main.preparation.purchaseCost);
        assertFalse(evaluate(MethodIntelligenceTest.data(0, 349,
                Collections.emptyList(), Collections.emptyList()), prices(prices)).actionable());

        MethodReadiness uim = evaluate(MethodIntelligenceTest.data(2, -1,
                barsAndHammer(2353, "Steel bar"), Collections.emptyList()), prices(Collections.emptyMap()));
        assertFalse(uim.actionable());
        assertEquals(RequirementState.BLOCKED, uim.access);
    }

    @Test
    public void selectorUsesStructuredSmithingContractForF2pAndFailsClosedOnUnknownMode()
    {
        List<ItemState> inventory = barsAndHammer(2353, "Steel bar");
        GameData f2p = withMembership(MethodIntelligenceTest.data(0, -1,
                inventory, Collections.emptyList()), Membership.F2P, 80);
        TrainingMethodSelector selector = selector(prices(Collections.emptyMap()));
        TrainingPlan selected = selector.rankedCandidates(f2p, Skill.SMITHING, 80,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false).stream()
                .filter(p -> p.method().id.equals(method.id) && p.readiness.actionable())
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals("steel_platebody_smithing", selected.readiness.recipe.id);
        assertEquals(BankingMode.CONVENTIONAL_BANK_LOOP, selected.strategyProfile.bankingBehavior);
        GameData unknown = withMembership(MethodIntelligenceTest.data(99, -1,
                inventory, Collections.emptyList()), Membership.F2P, 80);
        assertTrue(selector.rankedCandidates(unknown, Skill.SMITHING, 80,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false).stream()
                .noneMatch(p -> p.method().id.equals(method.id)));
    }

    private MethodReadiness evaluate(GameData data, AccountResourcePlanner resources)
    {
        return new MethodReadinessService(catalog, new MethodPreparationService(resources))
                .evaluate(data, method, false);
    }

    private TrainingMethodSelector selector(AccountResourcePlanner resources)
    {
        return new TrainingMethodSelector(new TrainingMethodCatalog(), null,
                new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(),
                new UimInventoryResolutionService(),
                new MethodReadinessService(catalog, new MethodPreparationService(resources)));
    }

    private static AccountResourcePlanner prices(Map<String, Integer> values)
    {
        return new AccountResourcePlanner(new MarketPriceService(null)
        {
            @Override public MarketPriceQuote quote(String name)
            {
                Integer price = values.get(name);
                int id = name.equals("Steel bar") ? 2353 : name.equals("Hammer") ? 2347 : -1;
                return price == null || id < 0 ? null : new MarketPriceQuote(id, name, price);
            }
        }, new ResourceSourceCatalog());
    }

    private static List<ItemState> barsAndHammer(int id, String name)
    {
        List<ItemState> inventory = bars(id, name, 25);
        inventory.add(new ItemState(2347, "Hammer", 1));
        return inventory;
    }

    private static List<ItemState> bars(int id, String name, int count)
    { return MethodIntelligenceTest.carried(id, name, count); }

    private static GameData withMembership(GameData source, Membership membership, int level)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) levels.put(skill, level);
        AccountSnapshot account = new AccountSnapshot("test", 1,
                source.account().modeCode(), source.account().accountTypeName, membership,
                1, 1, 0, levels, Collections.emptyMap());
        return GameData.builder(account).inventory(source.inventory()).equipment(source.equipment())
                .bank(source.bank()).economy(source.economy()).build();
    }
}
