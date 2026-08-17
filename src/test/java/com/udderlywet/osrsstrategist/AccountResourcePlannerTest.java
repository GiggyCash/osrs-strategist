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
        StrategyDataBundle data = StrategyDataBundle.builder(account(0))
                .inventory(new InventorySnapshot(Collections.singletonList(
                        item("Yew logs", 100))))
                .build();

        AccountResourcePlan plan = planner.plan(
                data,
                Collections.singletonList(need("Yew logs", 500)),
                false);

        assertFalse(plan.isPrimaryStorageObserved());
        assertFalse(plan.isFullySupplied());
        assertTrue(plan.getGuidance().contains("Open your bank once"));
        assertFalse(plan.getGuidance().contains("Buy 400"));
    }

    @Test
    public void mainGetsExactShortfallAfterBankObservation()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account(0))
                .inventory(new InventorySnapshot(Collections.singletonList(
                        item("Yew logs", 25))))
                .bank(new BankSnapshot(Collections.singletonList(
                        item("Yew logs", 275)), 1L))
                .build();

        AccountResourcePlan plan = planner.plan(
                data,
                Collections.singletonList(need("Yew logs", 500)),
                false);

        assertTrue(plan.isPrimaryStorageObserved());
        assertEquals(200, plan.getTotalMissingUnits());
        assertTrue(plan.getGuidance().contains("Verified usable: 300 Yew logs"));
        assertTrue(plan.getGuidance().contains("Buy 200 Yew logs"));
        assertTrue(plan.getGuidance().contains("Grand Exchange"));
    }

    @Test
    public void ironSelfSourcesInsteadOfUsingGrandExchange()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account(1))
                .bank(new BankSnapshot(Collections.singletonList(
                        item("Ranarr weed", 40)), 1L))
                .build();

        AccountResourcePlan plan = planner.plan(
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
        StrategyDataBundle data = StrategyDataBundle.builder(account(4))
                .bank(new BankSnapshot(Collections.singletonList(
                        item("Oak plank", 100)), 1L))
                .groupStorage(new GroupStorageSnapshot(true,
                        Collections.singletonList(item("Oak plank", 300))))
                .build();

        List<ResolvedMethodInput> needs = Collections.singletonList(
                need("Oak plank", 500));
        AccountResourcePlan disabled = planner.plan(data, needs, false);
        AccountResourcePlan enabled = planner.plan(data, needs, true);

        assertEquals(400, disabled.getTotalMissingUnits());
        assertEquals(100, enabled.getTotalMissingUnits());
        assertTrue(enabled.getGuidance().contains("Group Storage"));
    }

    @Test
    public void enabledButUnobservedGroupStorageIsNeverAssumedEmpty()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account(4))
                .bank(new BankSnapshot(Collections.singletonList(
                        item("Oak plank", 100)), 1L))
                .groupStorage(GroupStorageSnapshot.unknown())
                .build();

        AccountResourcePlan plan = planner.plan(
                data,
                Collections.singletonList(need("Oak plank", 500)),
                true);

        assertEquals(400, plan.getTotalMissingUnits());
        assertTrue(plan.getGuidance().contains("enabled but unobserved"));
    }

    @Test
    public void uimIgnoresNormalBankAndReportsRetrievalOnlySupplySeparately()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.LOOTING_BAG, CapabilityState.VERIFIED);
        Map<StorageCapability, List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.LOOTING_BAG,
                Collections.singletonList(item("Mahogany plank", 250)));

        StrategyDataBundle data = StrategyDataBundle.builder(account(2))
                .inventory(new InventorySnapshot(Collections.singletonList(
                        item("Mahogany plank", 20))))
                .bank(new BankSnapshot(Collections.singletonList(
                        item("Mahogany plank", 5000)), 1L))
                .storage(new StorageSnapshot(states, contents))
                .build();

        AccountResourcePlan plan = planner.plan(
                data,
                Collections.singletonList(need("Mahogany plank", 100)),
                false);

        assertEquals(80, plan.getTotalMissingUnits());
        ResourcePlanEntry entry = plan.getEntries().get(0);
        assertEquals(20, entry.getUsableOwned());
        assertEquals(250, entry.getRestrictedOwned());
        assertTrue(plan.getGuidance().contains("just in time"));
        assertTrue(plan.getGuidance().contains("retrieval-only UIM storage"));
        assertFalse(plan.getGuidance().contains("5,020"));
    }

    @Test
    public void equippedElementalStaffWaivesMatchingRuneConsumption()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account(0))
                .equipment(new EquipmentSnapshot(Collections.singletonList(
                        item("Staff of fire", 1))))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .build();

        AccountResourcePlan plan = planner.plan(
                data,
                Arrays.asList(
                        need("Nature rune", 100),
                        need("Fire rune", 500)),
                false);

        assertEquals(100, plan.getTotalMissingUnits());
        assertEquals(0, plan.getEntries().get(1).getMissing());
        assertTrue(plan.getGuidance().contains("Fire rune supplied by Staff of fire"));
        assertTrue(plan.getGuidance().contains("Buy 100 Nature rune"));
    }

    @Test
    public void emptyTomeOfFireDoesNotWaiveFireRunes()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account(0))
                .equipment(new EquipmentSnapshot(Collections.singletonList(
                        item("Tome of fire (empty)", 1))))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .build();

        AccountResourcePlan plan = planner.plan(
                data,
                Collections.singletonList(need("Fire rune", 500)),
                false);

        assertEquals(500, plan.getTotalMissingUnits());
    }

    @Test
    public void duplicateRecipeRowsAreMergedBeforeShortfallMath()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account(1))
                .bank(new BankSnapshot(Collections.singletonList(
                        item("Feather", 15)), 1L))
                .build();

        AccountResourcePlan plan = planner.plan(
                data,
                Arrays.asList(need("Feather", 10), need("feather", 20)),
                false);

        assertEquals(1, plan.getEntries().size());
        assertEquals(30, plan.getEntries().get(0).getRequired());
        assertEquals(15, plan.getEntries().get(0).getMissing());
    }

    private static ResolvedMethodInput need(String name, int quantity)
    {
        return new ResolvedMethodInput(name, -1, quantity);
    }

    private static ItemStackSnapshot item(String name, int quantity)
    {
        return new ItemStackSnapshot(-1, name, quantity);
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
}
