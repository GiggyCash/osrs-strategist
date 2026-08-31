package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QuestResourceDependencyIntegrationTest
{
    @Test
    public void dependencyCatalogResolvesNamedOutputsCaseInsensitively()
    {
        ResourceDependencyDefinition definition =
                new ResourceDependencyCatalog().forItemName("molten GLASS");

        assertNotNull(definition);
        assertEquals(ItemID.MOLTEN_GLASS, definition.getItemId());
        assertEquals("Molten glass", definition.getItemName());
    }

    @Test
    public void observedInventoryAndBankCombineBeforeDeclaringShortfall()
    {
        ItemsState inventory = new ItemsState(Collections.singletonList(
                new ItemState(ItemID.STEEL_BAR, "Steel bar", 2)));
        ItemsState bank = new ItemsState(Collections.singletonList(
                new ItemState(ItemID.STEEL_BAR, "Steel bar", 2)), 1L);

        AcquisitionPlan result = new ResourceAcquisitionPlanner().plan(
                context(bank, inventory),
                new ResourceNeed(ItemID.STEEL_BAR, "Steel bar", 3));

        assertEquals(Confidence.VERIFIED, result.getConfidence());
        assertEquals(4, result.getConfirmedQuantity());
        assertEquals(AcquisitionSource.BANK, result.getSource());
    }

    @Test
    public void provenShortfallDoesNotSubtractOwnedQuantityTwice()
    {
        ItemsState bank = new ItemsState(Collections.singletonList(
                new ItemState(ItemID.MOLTEN_GLASS, "Molten glass", 5)), 1L);
        StrategyContext context = context(bank);

        DependencyResolution result = new ResourceAcquisitionPlanner()
                .resolveKnownShortfall(context, "Molten glass", 3);

        assertNotNull(result);
        assertTrue(result.getNodes().stream().anyMatch(node ->
                node.getId().equals("resource:" + ItemID.BUCKET_SAND)));
        ResolvedDependencyNode root = result.getNodes().stream()
                .filter(node -> node.getId().equals(
                        "resource:" + ItemID.MOLTEN_GLASS))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(3, root.getRequiredQuantity());
    }

    @Test
    public void partialOwnedDependencyOnlyExpandsMissingRecipeBatches()
    {
        ItemsState bank = new ItemsState(Collections.singletonList(
                new ItemState(ItemID.STEEL_BAR, "Steel bar", 24)), 1L);

        DependencyResolution result = new ResourceAcquisitionPlanner()
                .resolveDependencies(context(bank),
                        new ResourceNeed(ItemID.MCANNONBALL, "Cannonball", 100));

        ResolvedDependencyNode iron = result.getNodes().stream()
                .filter(node -> node.getId().equals("resource:" + ItemID.IRON_ORE))
                .findFirst().orElseThrow(AssertionError::new);
        ResolvedDependencyNode coal = result.getNodes().stream()
                .filter(node -> node.getId().equals("resource:" + ItemID.COAL))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(1, iron.getRequiredQuantity());
        assertEquals(2, coal.getRequiredQuantity());
    }

    @Test
    public void questPreparationPromotesFirstRecursiveResourceStep()
    {
        ItemRequirementExpression requirement = ItemRequirementExpression.item(
                "Molten glass", 8, ItemRequirementScope.OWNED_OR_RETRIEVABLE);
        QuestDefinition quest = new QuestDefinition("Dependency test quest", false,
                Collections.emptyList(), Collections.<Skill, Integer>emptyMap(),
                Collections.emptyList(), requirement, 0, Collections.emptyList(),
                "Test location", Collections.singletonList("Test unlock"),
                Collections.<Skill, Integer>emptyMap());
        ItemsState bank = new ItemsState(Collections.singletonList(
                new ItemState(ItemID.MOLTEN_GLASS, "Molten glass", 5)), 1L);

        QuestResolution result = new QuestRequirementResolver().resolve(
                quest, context(bank));

        assertNotNull(result);
        assertTrue(result.getGuidance().getAction().toLowerCase().contains("sand"));
        assertTrue(result.getGuidance().getSupplies().contains(
                "Confirmed shortfall: 3 × Molten glass"));
        assertTrue(result.getGuidance().getSupplies().contains(
                "Dependency first step for 3 × Molten glass"));
    }

    private static StrategyContext context(ItemsState bank)
    {
        return context(bank, new ItemsState(Collections.emptyList()));
    }

    private static StrategyContext context(ItemsState bank,
            ItemsState inventory)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Dependency", 1,
                AccountMode.IRONMAN.name(), MembershipStatus.P2P,
                1, 1500, 0L, levels, xp);
        GameData data = GameData.builder(account)
                .inventory(inventory)
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(bank)
                .quests(new QuestSnapshot(Collections.emptyMap()))
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.QUICK_20_MIN, QuestTolerance.NORMAL, GoalType.MAX,
                false, false, false, new PreferenceProfile());
    }
}
