package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LiveRunePouchStateReaderTest
{
    @Test
    public void detectsNormalAndDivinePouchesByObservedInventoryName()
    {
        assertTrue(LiveRunePouchStateReader.hasUsableRunePouch(
                inventory("Rune pouch")));
        assertTrue(LiveRunePouchStateReader.hasUsableRunePouch(
                inventory("Divine rune pouch")));
        assertTrue(LiveRunePouchStateReader.hasUsableRunePouch(
                inventory("Rune pouch (l)")));
        assertFalse(LiveRunePouchStateReader.hasUsableRunePouch(
                inventory("Rune pouch note")));
    }

    @Test
    public void liveContentsReplaceStaleRunePouchAndPreserveOtherStorage()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.RUNE_POUCH, CapabilityState.VERIFIED);
        states.put(StorageCapability.DEATH_STORAGE, CapabilityState.VERIFIED);
        Map<StorageCapability, List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.RUNE_POUCH,
                Collections.singletonList(item(554, "Fire rune", 10)));
        contents.put(StorageCapability.DEATH_STORAGE,
                Collections.singletonList(item(1, "Stored item", 1)));
        StorageSnapshot base = new StorageSnapshot(states, contents);

        StorageSnapshot merged = LiveRunePouchStateReader.mergeObserved(
                base,
                true,
                Arrays.asList(
                        item(561, "Nature rune", 600),
                        item(554, "Fire rune", 3000)));

        assertTrue(merged.verified(StorageCapability.RUNE_POUCH));
        assertEquals(2, merged.contentsOf(StorageCapability.RUNE_POUCH).size());
        assertEquals(600, merged.quantityOf(StorageCapability.RUNE_POUCH, 561));
        assertEquals(1, merged.quantityOf(StorageCapability.DEATH_STORAGE, 1));
    }

    @Test
    public void absentPouchRemovesStalePouchWithoutErasingDeathStorage()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.RUNE_POUCH, CapabilityState.VERIFIED);
        states.put(StorageCapability.DEATH_STORAGE, CapabilityState.VERIFIED);
        Map<StorageCapability, List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.RUNE_POUCH,
                Collections.singletonList(item(561, "Nature rune", 5000)));
        contents.put(StorageCapability.DEATH_STORAGE,
                Collections.singletonList(item(1, "Stored item", 1)));

        StorageSnapshot merged = LiveRunePouchStateReader.mergeObserved(
                new StorageSnapshot(states, contents), false, null);

        assertFalse(merged.hasObservedContents(StorageCapability.RUNE_POUCH));
        assertEquals(CapabilityState.UNKNOWN,
                merged.stateOf(StorageCapability.RUNE_POUCH));
        assertEquals(1, merged.quantityOf(StorageCapability.DEATH_STORAGE, 1));
    }

    @Test
    public void runePouchRunesCountAsDirectlyUsableForUim()
    {
        StorageSnapshot storage = LiveRunePouchStateReader.mergeObserved(
                StorageSnapshot.unknown(), true,
                Arrays.asList(
                        item(561, "Nature rune", 1000),
                        item(554, "Fire rune", 5000)));
        StrategyDataBundle data = StrategyDataBundle.builder(account(2))
                .inventory(inventory("Rune pouch"))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .storage(storage)
                .build();

        ObservedItemIndex items = new ObservedItemIndex(data, true);
        assertEquals(1000, items.quantity("Nature rune"));
        assertEquals(5000, items.quantity("Fire rune"));
        assertEquals(0, items.restrictedQuantity("Nature rune"));
    }

    private static InventorySnapshot inventory(String name)
    {
        // The detector intentionally rejects generic notes and only accepts an
        // item whose observed name includes the actual Rune pouch phrase. The
        // fake note case below therefore uses quantity zero so it cannot appear
        // as a usable inventory item.
        int quantity = "Rune pouch note".equals(name) ? 0 : 1;
        return new InventorySnapshot(Collections.singletonList(
                item(12791, name, quantity)));
    }

    private static ItemStackSnapshot item(int id, String name, int quantity)
    {
        return new ItemStackSnapshot(id, name, quantity);
    }

    private static AccountSnapshot account(int typeCode)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0L;
        for (Skill skill : Skill.values())
        {
            int level = skill == Skill.HITPOINTS ? 80 : 70;
            levels.put(skill, level);
            int value = Experience.getXpForLevel(level);
            xp.put(skill, value);
            total += level;
            totalXp += value;
        }
        return new AccountSnapshot(
                "Rune Pouch Test", typeCode,
                AccountMode.fromTypeCode(typeCode).name(),
                MembershipStatus.P2P, 1, total, totalXp, levels, xp);
    }
}
