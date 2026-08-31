package compass;

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
        GameData data = bundle(0)
                .bank(new ItemsState(Arrays.asList(item("Spade", 1),
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
        GameData data = bundle(0).inventory(new ItemsState(
                Collections.singletonList(item("Emerald lantern", 1)))).build();
        assertTrue(evaluator.evaluate(requirement, data, false).isSatisfied());
    }

    @Test
    public void unobservedBankRemainsCheckNeeded()
    {
        ItemRequirementResult result = evaluator.evaluate(
                ItemRequirementExpression.item("Rope", 1,
                        ItemRequirementScope.IMMEDIATELY_USABLE),
                bundle(0).inventory(new ItemsState(
                        Collections.emptyList())).build(), false);
        assertEquals(RequirementState.CHECK_NEEDED, result.getState());
        assertTrue(result.getAction().startsWith("Check whether you own"));
    }

    @Test
    public void groupStorageCountsOnlyWhenObservedAndEnabled()
    {
        ItemRequirementExpression requirement = ItemRequirementExpression.item(
                "Rope", 1, ItemRequirementScope.IMMEDIATELY_USABLE);
        GameData data = bundle(4)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .groupStorage(new ItemsState(true,
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
        Map<StorageCapability, java.util.List<ItemState>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.LOOTING_BAG,
                Collections.singletonList(item("Rope", 1)));
        GameData data = bundle(2)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.singletonList(item("Rope", 1)), 1L))
                .storage(new StorageSnapshot(states, contents)).build();
        assertEquals(RequirementState.BLOCKED,
                evaluator.evaluate(usable, data, false).getState());

        ItemRequirementExpression retrievable = ItemRequirementExpression.item(
                "Rope", 1, ItemRequirementScope.OWNED_OR_RETRIEVABLE);
        assertTrue(evaluator.evaluate(retrievable, data, false).isSatisfied());
    }

    @Test
    public void partialUimSnapshotLeavesUsableOwnershipUnknown()
    {
        GameData data = bundle(2)
                .inventory(new ItemsState(Collections.emptyList()))
                .build();
        ItemRequirementResult result = evaluator.evaluate(
                ItemRequirementExpression.item("Rope", 1,
                        ItemRequirementScope.IMMEDIATELY_USABLE),
                data, false);

        assertEquals(RequirementState.CHECK_NEEDED, result.getState());
        assertTrue(result.getAction().startsWith("Check whether you own"));
    }

    @Test
    public void equippedRequirementCannotBeSatisfiedByBankedItem()
    {
        ItemRequirementExpression requirement = ItemRequirementExpression.item(
                "Anti-dragon shield", 1, ItemRequirementScope.EQUIPPED);
        GameData data = bundle(0)
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.singletonList(
                        item("Anti-dragon shield", 1)), 1L)).build();
        assertEquals(RequirementState.BLOCKED,
                evaluator.evaluate(requirement, data, false).getState());
    }

    @Test
    public void stackableEquippedQuantitySatisfiesExactRequirement()
    {
        ItemRequirementExpression equipped = ItemRequirementExpression.item(
                "Broad bolts", 50, ItemRequirementScope.EQUIPPED);
        GameData data = bundle(0)
                .equipment(new ItemsState(Collections.singletonList(
                        item("Broad bolts", 75)))).build();
        assertTrue(evaluator.evaluate(equipped, data, false).isSatisfied());
    }

    @Test
    public void carriedAndEquippedStacksCombineForExactRequirement()
    {
        ItemRequirementExpression available = ItemRequirementExpression.item(
                "Broad bolts", 100, ItemRequirementScope.CARRIED_OR_EQUIPPED);
        GameData data = bundle(0)
                .inventory(new ItemsState(Collections.singletonList(
                        item("Broad bolts", 40))))
                .equipment(new ItemsState(Collections.singletonList(
                        item("Broad bolts", 60)))).build();
        assertTrue(evaluator.evaluate(available, data, false).isSatisfied());
    }

    @Test
    public void nameObservableClassUsesObservedItemsAndExclusions()
    {
        ItemRequirementExpression axe = ItemRequirementExpression.itemClass(
                ItemRequirementClass.AXE, 1,
                ItemRequirementScope.IMMEDIATELY_USABLE);
        GameData data = bundle(0)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Arrays.asList(item("Rune pickaxe", 1),
                        item("Dragon axe", 1)), 1L)).build();
        assertTrue(evaluator.evaluate(axe, data, false).isSatisfied());

        ItemRequirementExpression cat = ItemRequirementExpression.itemClass(
                ItemRequirementClass.CAT_OR_KITTEN, 1,
                ItemRequirementScope.IMMEDIATELY_USABLE, "Overgrown cat");
        GameData overgrownOnly = bundle(0)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.singletonList(
                        item("Overgrown cat", 1)), 1L)).build();
        assertEquals(RequirementState.BLOCKED,
                evaluator.evaluate(cat, overgrownOnly, false).getState());
    }

    @Test
    public void mechanicalClassStaysExplicitCheckNeeded()
    {
        ItemRequirementExpression light = ItemRequirementExpression.itemClass(
                ItemRequirementClass.LIGHT_SOURCE, 1,
                ItemRequirementScope.OWNED_OR_RETRIEVABLE);
        GameData data = bundle(0)
                .bank(new ItemsState(Collections.singletonList(
                        item("Bullseye lantern", 1)), 1L)).build();
        ItemRequirementResult result = evaluator.evaluate(light, data, false);
        assertEquals(RequirementState.CHECK_NEEDED, result.getState());
        assertTrue(result.getAction().contains("suitable light source"));
    }

    @Test
    public void explicitVerificationNodeCannotClaimSatisfied()
    {
        ItemRequirementResult result = evaluator.evaluate(
                ItemRequirementExpression.checkNeeded(
                        "Check the route-specific item requirement"),
                bundle(0).bank(new ItemsState(Collections.emptyList(), 1L))
                        .build(), false);
        assertEquals(RequirementState.CHECK_NEEDED, result.getState());
        assertEquals("Check the route-specific item requirement",
                result.getAction());
    }

    @Test
    public void exactQuestInventorySlotsUseObservedLayout()
    {
        ItemRequirementExpression slots = ItemRequirementExpression.itemClass(
                ItemRequirementClass.EMPTY_INVENTORY_SPACE, 5,
                ItemRequirementScope.CARRIED);
        java.util.List<ItemState> twentyFour = new java.util.ArrayList<>();
        for (int slot = 0; slot < 24; slot++)
            twentyFour.add(new ItemState(20_000 + slot,
                    "Persistent " + slot, 1, slot));
        ItemRequirementResult blocked = evaluator.evaluate(slots,
                bundle(2).inventory(new ItemsState(twentyFour, true))
                        .build(), false);
        assertEquals(RequirementState.BLOCKED, blocked.getState());
        assertTrue(blocked.getAction().contains("only 4 are observed"));

        twentyFour.remove(twentyFour.size() - 1);
        assertTrue(evaluator.evaluate(slots,
                bundle(2).inventory(new ItemsState(twentyFour, true))
                        .build(), false).isSatisfied());
    }

    private static GameData.Builder bundle(int type)
    {
        EnumMap<Skill, Integer> levels = new EnumMap<>(Skill.class);
        EnumMap<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 99); xp.put(skill, 0); }
        return GameData.builder(new AccountSnapshot("Player", type,
                "test", MembershipStatus.P2P, 0, 2277, 0, levels, xp));
    }

    private static ItemState item(String name, int quantity)
    {
        return new ItemState(name.hashCode(), name, quantity);
    }
}
