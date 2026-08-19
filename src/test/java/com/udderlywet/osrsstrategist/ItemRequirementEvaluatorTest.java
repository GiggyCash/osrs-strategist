package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ItemRequirementEvaluatorTest
{
    private final ItemRequirementEvaluator evaluator = new ItemRequirementEvaluator();

    @Test
    public void allOfAndSubstitutesUseObservedEvidence()
    {
        ItemRequirementExpression requirement = ItemRequirementExpression.allOf(
                ItemRequirementExpression.item("Spade", 1,
                        ItemRequirementScope.IMMEDIATELY_USABLE),
                ItemRequirementExpression.item("Bronze axe", 1,
                        ItemRequirementScope.IMMEDIATELY_USABLE, "Iron axe"));
        StrategyDataBundle data = bundle(0)
                .bank(new BankSnapshot(Arrays.asList(item("Spade", 1),
                        item("Iron axe", 1)), 1L)).build();
        assertTrue(evaluator.evaluate(requirement, data, false).isSatisfied());
    }

    @Test
    public void anyOfProducesOneConcreteBranch()
    {
        ItemRequirementExpression requirement = ItemRequirementExpression.anyOf(
                ItemRequirementExpression.item("Sapphire lantern", 1,
                        ItemRequirementScope.CARRIED),
                ItemRequirementExpression.item("Emerald lantern", 1,
                        ItemRequirementScope.CARRIED));
        StrategyDataBundle data = bundle(0).inventory(new InventorySnapshot(
                Collections.singletonList(item("Emerald lantern", 1)))).build();
        assertTrue(evaluator.evaluate(requirement, data, false).isSatisfied());
    }

    @Test
    public void unobservedBankRemainsCheckNeeded()
    {
        ItemRequirementResult result = evaluator.evaluate(
                ItemRequirementExpression.item("Rope", 1,
                        ItemRequirementScope.IMMEDIATELY_USABLE),
                bundle(0).inventory(new InventorySnapshot(
                        Collections.emptyList())).build(), false);
        assertEquals(RequirementState.CHECK_NEEDED, result.getState());
        assertTrue(result.getAction().startsWith("Check whether you own"));
    }

    @Test
    public void groupStorageCountsOnlyWhenObservedAndEnabled()
    {
        ItemRequirementExpression requirement = ItemRequirementExpression.item(
                "Rope", 1, ItemRequirementScope.IMMEDIATELY_USABLE);
        StrategyDataBundle data = bundle(4)
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .groupStorage(new GroupStorageSnapshot(true,
                        Collections.singletonList(item("Rope", 1)))).build();
        assertEquals(RequirementState.BLOCKED,
                evaluator.evaluate(requirement, data, false).getState());
        assertTrue(evaluator.evaluate(requirement, data, true).isSatisfied());
    }

    @Test
    public void uimBankNeverSatisfiesAndRetrievalIsExplicit()
    {
        ItemRequirementExpression usable = ItemRequirementExpression.item(
                "Rope", 1, ItemRequirementScope.IMMEDIATELY_USABLE);
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.LOOTING_BAG, CapabilityState.VERIFIED);
        Map<StorageCapability, java.util.List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.LOOTING_BAG,
                Collections.singletonList(item("Rope", 1)));
        StrategyDataBundle data = bundle(2)
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.singletonList(item("Rope", 1)), 1L))
                .storage(new StorageSnapshot(states, contents)).build();
        assertEquals(RequirementState.BLOCKED,
                evaluator.evaluate(usable, data, false).getState());

        ItemRequirementExpression retrievable = ItemRequirementExpression.item(
                "Rope", 1, ItemRequirementScope.OWNED_OR_RETRIEVABLE);
        assertTrue(evaluator.evaluate(retrievable, data, false).isSatisfied());
    }

    @Test
    public void equippedRequirementCannotBeSatisfiedByBankedItem()
    {
        ItemRequirementExpression requirement = ItemRequirementExpression.item(
                "Anti-dragon shield", 1, ItemRequirementScope.EQUIPPED);
        StrategyDataBundle data = bundle(0)
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.singletonList(
                        item("Anti-dragon shield", 1)), 1L)).build();
        assertEquals(RequirementState.BLOCKED,
                evaluator.evaluate(requirement, data, false).getState());
    }

    @Test
    public void stackableEquippedQuantitySatisfiesExactRequirement()
    {
        ItemRequirementExpression equipped = ItemRequirementExpression.item(
                "Broad bolts", 50, ItemRequirementScope.EQUIPPED);
        StrategyDataBundle data = bundle(0)
                .equipment(new EquipmentSnapshot(Collections.singletonList(
                        item("Broad bolts", 75)))).build();
        assertTrue(evaluator.evaluate(equipped, data, false).isSatisfied());
    }

    @Test
    public void carriedAndEquippedStacksCombineForExactRequirement()
    {
        ItemRequirementExpression available = ItemRequirementExpression.item(
                "Broad bolts", 100, ItemRequirementScope.CARRIED_OR_EQUIPPED);
        StrategyDataBundle data = bundle(0)
                .inventory(new InventorySnapshot(Collections.singletonList(
                        item("Broad bolts", 40))))
                .equipment(new EquipmentSnapshot(Collections.singletonList(
                        item("Broad bolts", 60)))).build();
        assertTrue(evaluator.evaluate(available, data, false).isSatisfied());
    }

    private static StrategyDataBundle.Builder bundle(int type)
    {
        EnumMap<Skill, Integer> levels = new EnumMap<>(Skill.class);
        EnumMap<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 99); xp.put(skill, 0); }
        return StrategyDataBundle.builder(new AccountSnapshot("Player", type,
                "test", MembershipStatus.P2P, 0, 2277, 0, levels, xp));
    }

    private static ItemStackSnapshot item(String name, int quantity)
    {
        return new ItemStackSnapshot(name.hashCode(), name, quantity);
    }
}
