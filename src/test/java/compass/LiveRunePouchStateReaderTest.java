package compass;

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
    public void detectsOnlyKnownUsablePouchNames()
    {
        assertTrue(LiveRunePouchStateReader.hasUsableRunePouch(
                inventory("Rune pouch", 1)));
        assertTrue(LiveRunePouchStateReader.hasUsableRunePouch(
                inventory("Divine rune pouch", 1)));
        assertFalse(LiveRunePouchStateReader.hasUsableRunePouch(
                inventory("Rune pouch note", 1)));
        assertFalse(LiveRunePouchStateReader.hasUsableRunePouch(
                inventory("Future rune pouch token", 1)));
    }

    @Test
    public void liveContentsReplaceStaleRunePouchAndPreserveOtherStorage()
    {
        Map<StorageKind, Capability> states =
                new EnumMap<>(StorageKind.class);
        states.put(StorageKind.RUNE_POUCH, Capability.VERIFIED);
        states.put(StorageKind.DEATH_STORAGE, Capability.VERIFIED);
        Map<StorageKind, List<ItemState>> contents =
                new EnumMap<>(StorageKind.class);
        contents.put(StorageKind.RUNE_POUCH,
                Collections.singletonList(item(554, "Fire rune", 10)));
        contents.put(StorageKind.DEATH_STORAGE,
                Collections.singletonList(item(1, "Stored item", 1)));
        StorageSnapshot base = new StorageSnapshot(states, contents);

        StorageSnapshot merged = LiveRunePouchStateReader.mergeObserved(
                base,
                true,
                Arrays.asList(
                        item(561, "Nature rune", 600),
                        item(554, "Fire rune", 3000)));

        assertTrue(merged.verified(StorageKind.RUNE_POUCH));
        assertEquals(2, merged.contentsOf(StorageKind.RUNE_POUCH).size());
        assertEquals(600, merged.quantityOf(StorageKind.RUNE_POUCH, 561));
        assertEquals(1, merged.quantityOf(StorageKind.DEATH_STORAGE, 1));
    }

    @Test
    public void absentPouchRemovesStalePouchWithoutErasingDeathStorage()
    {
        Map<StorageKind, Capability> states =
                new EnumMap<>(StorageKind.class);
        states.put(StorageKind.RUNE_POUCH, Capability.VERIFIED);
        states.put(StorageKind.DEATH_STORAGE, Capability.VERIFIED);
        Map<StorageKind, List<ItemState>> contents =
                new EnumMap<>(StorageKind.class);
        contents.put(StorageKind.RUNE_POUCH,
                Collections.singletonList(item(561, "Nature rune", 5000)));
        contents.put(StorageKind.DEATH_STORAGE,
                Collections.singletonList(item(1, "Stored item", 1)));

        StorageSnapshot merged = LiveRunePouchStateReader.mergeObserved(
                new StorageSnapshot(states, contents), false, null);

        assertFalse(merged.hasObservedContents(StorageKind.RUNE_POUCH));
        assertEquals(Capability.UNKNOWN,
                merged.stateOf(StorageKind.RUNE_POUCH));
        assertEquals(1, merged.quantityOf(StorageKind.DEATH_STORAGE, 1));
    }

    @Test
    public void runePouchRunesCountAsDirectlyUsableForUim()
    {
        StorageSnapshot storage = LiveRunePouchStateReader.mergeObserved(
                StorageSnapshot.unknown(), true,
                Arrays.asList(
                        item(561, "Nature rune", 1000),
                        item(554, "Fire rune", 5000)));
        GameData data = GameData.builder(account(2))
                .inventory(inventory("Rune pouch", 1))
                .equipment(new ItemsState(Collections.emptyList()))
                .storage(storage)
                .build();

        ItemIndex items = new ItemIndex(data, true);
        assertEquals(1000, items.quantity("Nature rune"));
        assertEquals(5000, items.quantity("Fire rune"));
        assertEquals(0, items.restrictedQuantity("Nature rune"));
    }

    private static ItemsState inventory(String name, int quantity)
    {
        return new ItemsState(Collections.singletonList(
                item(12791, name, quantity)));
    }

    private static ItemState item(int id, String name, int quantity)
    {
        return new ItemState(id, name, quantity);
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
        return new AccountSnapshot("Rune Pouch Test", 0L, typeCode, AccountMode.fromTypeCode(typeCode).name(), Membership.P2P, 1, total, totalXp, levels, xp);
    }
}
