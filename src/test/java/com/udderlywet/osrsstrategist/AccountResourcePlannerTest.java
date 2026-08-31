package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountResourcePlannerTest
{
    private final AccountResourcePlanner planner = new AccountResourcePlanner();

    @Test
    public void unopenedMainBankNeverBecomesFakeShortfall()
    {
        GameData data = GameData.builder(account(0))
                .inventory(new ItemsState(Collections.singletonList(
                        item("Yew logs", 100))))
                .build();

        SupplyPlan plan = planner.plan(
                data,
                Collections.singletonList(need("Yew logs", 500)),
                false);

        assertFalse(plan.isPrimaryStorageObserved());
        assertFalse(plan.isFullySupplied());
        assertTrue(plan.getGuidance().contains("Open your bank once"));
        assertFalse(plan.getGuidance().contains("Buy 400"));
    }

    @Test
    public void mainDoesNotAssumeAnUnpricedShortfallShouldBeBought()
    {
        GameData data = GameData.builder(account(0))
                .inventory(new ItemsState(Collections.singletonList(
                        item("Yew logs", 25))))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.singletonList(
                        item("Yew logs", 275)), 1L))
                .build();

        SupplyPlan plan = planner.plan(
                data,
                Collections.singletonList(need("Yew logs", 500)),
                false);

        assertTrue(plan.isPrimaryStorageObserved());
        assertEquals(200, plan.getTotalMissingUnits());
        assertTrue(plan.getGuidance().contains("Verified usable: 300 Yew logs"));
        assertTrue(plan.getGuidance().contains("Do not assume the shortfall should be bought"));
        assertTrue(plan.getGuidance().contains("Reviewed self-source route"));
    }

    @Test
    public void ironSelfSourcesInsteadOfUsingGrandExchange()
    {
        GameData data = GameData.builder(account(1))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.singletonList(
                        item("Ranarr weed", 40)), 1L))
                .build();

        SupplyPlan plan = planner.plan(
                data,
                Collections.singletonList(need("Ranarr weed", 100)),
                false);

        assertEquals(60, plan.getTotalMissingUnits());
        assertTrue(plan.getGuidance().contains("Self-source 60 Ranarr weed"));
        assertFalse(plan.getGuidance().contains("Grand Exchange"));
    }

    @Test
    public void groupStorageOnlyChangesShortfallWhenEnabledAndObserved()
    {
        GameData data = GameData.builder(account(4))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.singletonList(
                        item("Oak plank", 100)), 1L))
                .groupStorage(new ItemsState(true,
                        Collections.singletonList(item("Oak plank", 300))))
                .build();

        List<MethodInput> needs = Collections.singletonList(
                need("Oak plank", 500));
        SupplyPlan disabled = planner.plan(data, needs, false);
        SupplyPlan enabled = planner.plan(data, needs, true);

        assertEquals(400, disabled.getTotalMissingUnits());
        assertEquals(100, enabled.getTotalMissingUnits());
        assertTrue(enabled.getGuidance().contains("Group Storage"));
    }

    @Test
    public void enabledButUnobservedGroupStorageIsNeverAssumedEmpty()
    {
        GameData data = GameData.builder(account(4))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.singletonList(
                        item("Oak plank", 100)), 1L))
                .groupStorage(ItemsState.unknown())
                .build();

        SupplyPlan plan = planner.plan(
                data,
                Collections.singletonList(need("Oak plank", 500)),
                true);

        assertEquals(400, plan.getTotalMissingUnits());
        assertTrue(plan.getGuidance().contains(
                "enabled but has not been observed"));
    }

    @Test
    public void uimIgnoresNormalBankAndReportsRetrievalOnlySupplySeparately()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.LOOTING_BAG, CapabilityState.VERIFIED);
        Map<StorageCapability, List<ItemState>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.LOOTING_BAG,
                Collections.singletonList(item("Mahogany plank", 250)));

        GameData data = GameData.builder(account(2))
                .inventory(new ItemsState(Collections.singletonList(
                        item("Mahogany plank", 20))))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.singletonList(
                        item("Mahogany plank", 5000)), 1L))
                .storage(new StorageSnapshot(states, contents))
                .build();

        SupplyPlan plan = planner.plan(
                data,
                Collections.singletonList(need("Mahogany plank", 100)),
                false);

        assertEquals(80, plan.getTotalMissingUnits());
        ResourcePlanEntry entry = plan.getEntries().get(0);
        assertEquals(20, entry.getUsableOwned());
        assertEquals(250, entry.getRestrictedOwned());
        assertTrue(plan.getGuidance().contains("resupply only"));
        assertTrue(plan.getGuidance().contains("retrieval-only UIM storage"));
        assertFalse(plan.getGuidance().contains("5,020"));
    }

    @Test
    public void partialUimSnapshotNeverBecomesAProvenShortfall()
    {
        GameData data = GameData.builder(account(2))
                .inventory(new ItemsState(Collections.emptyList()))
                .build();

        SupplyPlan plan = planner.plan(data,
                Collections.singletonList(need("Oak plank", 100)), false);

        assertFalse(plan.isPrimaryStorageObserved());
        assertTrue(plan.getGuidance().contains("not fully observed"));
        assertFalse(plan.getGuidance().contains("Self-source 100"));
    }

    @Test
    public void equippedElementalStaffWaivesMatchingRuneConsumption()
    {
        GameData data = GameData.builder(account(0))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.singletonList(
                        item("Staff of fire", 1))))
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .build();

        SupplyPlan plan = planner.plan(
                data,
                Arrays.asList(
                        need("Nature rune", 100),
                        need("Fire rune", 500)),
                false);

        assertEquals(100, plan.getTotalMissingUnits());
        assertEquals(0, plan.getEntries().get(1).getMissing());
        assertTrue(plan.getGuidance().contains("Fire rune supplied by Staff of fire"));
        assertTrue(plan.getGuidance().contains("Do not assume the shortfall should be bought"));
    }

    @Test
    public void lowBurdenExactMainPurchaseUsesLivePriceAndLiquidCash()
    {
        AccountResourcePlanner pricedPlanner = new AccountResourcePlanner(
                new PurchaseCostAdvisor(new FixedPriceService(20)),
                new MainEconomyPlanner(), new ResourceSourceCatalog());
        GameData data = GameData.builder(account(0))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .economy(new AccountEconomySnapshot(100_000L, 100_000L,
                        Confidence.VERIFIED))
                .build();

        SupplyPlan plan = pricedPlanner.plan(data,
                Collections.singletonList(need("Yew logs", 100)), false);

        assertTrue(plan.getGuidance().contains("Buy 100 Yew logs"));
        assertTrue(plan.getGuidance().contains("2,000 coins"));
        assertTrue(plan.getGuidance().contains("low-burden"));
    }

    @Test
    public void wealthBurdenCanMakeMainUseReviewedSelfSourceRoute()
    {
        AccountResourcePlanner pricedPlanner = new AccountResourcePlanner(
                new PurchaseCostAdvisor(new FixedPriceService(500)),
                new MainEconomyPlanner(), new ResourceSourceCatalog());
        GameData data = GameData.builder(account(0))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .economy(new AccountEconomySnapshot(100_000L, 100_000L,
                        Confidence.VERIFIED))
                .build();

        SupplyPlan plan = pricedPlanner.plan(data,
                Collections.singletonList(need("Yew logs", 100)), false);

        assertTrue(plan.getGuidance().contains("Self-source 100 Yew logs"));
        assertTrue(plan.getGuidance().contains("Reviewed route"));
        assertFalse(plan.getGuidance().contains("Buy 100 Yew logs"));
    }

    @Test
    public void emptyTomeOfFireDoesNotWaiveFireRunes()
    {
        GameData data = GameData.builder(account(0))
                .equipment(new ItemsState(Collections.singletonList(
                        item("Tome of fire (empty)", 1))))
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .build();

        SupplyPlan plan = planner.plan(
                data,
                Collections.singletonList(need("Fire rune", 500)),
                false);

        assertEquals(500, plan.getTotalMissingUnits());
    }

    @Test
    public void duplicateRecipeRowsAreMergedBeforeShortfallMath()
    {
        GameData data = GameData.builder(account(1))
                .bank(new ItemsState(Collections.singletonList(
                        item("Feather", 15)), 1L))
                .build();

        SupplyPlan plan = planner.plan(
                data,
                Arrays.asList(need("Feather", 10), need("feather", 20)),
                false);

        assertEquals(1, plan.getEntries().size());
        assertEquals(30, plan.getEntries().get(0).getRequired());
        assertEquals(15, plan.getEntries().get(0).getMissing());
    }

    private static MethodInput need(String name, int quantity)
    {
        return new MethodInput(name, -1, quantity);
    }

    private static ItemState item(String name, int quantity)
    {
        return new ItemState(-1, name, quantity);
    }

    private static AccountSnapshot account(int typeCode)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(
                "Resource Test",
                typeCode,
                AccountMode.fromTypeCode(typeCode).name(),
                MembershipStatus.P2P,
                1,
                1500,
                0L,
                levels,
                xp);
    }

    private static final class FixedPriceService extends MarketPriceService
    {
        private final int price;

        private FixedPriceService(int price)
        {
            this.price = price;
        }

        @Override
        public MarketPriceQuote quote(String exactItemName)
        {
            return new MarketPriceQuote(1, exactItemName, price);
        }
    }
}
