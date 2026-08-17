package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;

/**
 * Reads currently usable Rune pouch contents from the same varbits and enum
 * mapping used by RuneLite's Clue Scroll plugin.
 *
 * <p>The pouch is live storage, not persistent ownership. If no recognized
 * usable pouch is observed in inventory, stale remembered pouch contents are
 * removed while every unrelated storage capability is preserved.</p>
 */
@Singleton
public class LiveRunePouchStateReader
{
    private static final int[] AMOUNT_VARBITS = {
            VarbitID.RUNE_POUCH_QUANTITY_1,
            VarbitID.RUNE_POUCH_QUANTITY_2,
            VarbitID.RUNE_POUCH_QUANTITY_3,
            VarbitID.RUNE_POUCH_QUANTITY_4
    };

    private static final int[] RUNE_VARBITS = {
            VarbitID.RUNE_POUCH_TYPE_1,
            VarbitID.RUNE_POUCH_TYPE_2,
            VarbitID.RUNE_POUCH_TYPE_3,
            VarbitID.RUNE_POUCH_TYPE_4
    };

    private final Client client;
    private final ItemManager itemManager;

    @Inject
    public LiveRunePouchStateReader(Client client, ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
    }

    public StorageSnapshot merge(
            StorageSnapshot base,
            InventorySnapshot inventory)
    {
        boolean usable = hasUsableRunePouch(inventory)
                && client.getGameState() == GameState.LOGGED_IN;
        List<ItemStackSnapshot> liveContents = usable
                ? readContents() : null;
        return mergeObserved(base, usable, liveContents);
    }

    /** Pure merge seam used by tests and protects unrelated remembered state. */
    static StorageSnapshot mergeObserved(
            StorageSnapshot base,
            boolean usablePouchObserved,
            List<ItemStackSnapshot> liveContents)
    {
        StorageSnapshot source = base == null
                ? StorageSnapshot.unknown() : base;
        EnumMap<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.putAll(source.getStates());
        EnumMap<StorageCapability, List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        for (Map.Entry<StorageCapability, List<ItemStackSnapshot>> entry
                : source.getObservedContents().entrySet())
        {
            contents.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        if (!usablePouchObserved)
        {
            states.remove(StorageCapability.RUNE_POUCH);
            contents.remove(StorageCapability.RUNE_POUCH);
            return new StorageSnapshot(states, contents);
        }

        states.put(StorageCapability.RUNE_POUCH, CapabilityState.VERIFIED);
        contents.put(StorageCapability.RUNE_POUCH,
                liveContents == null
                        ? new ArrayList<>()
                        : new ArrayList<>(liveContents));
        return new StorageSnapshot(states, contents);
    }

    List<ItemStackSnapshot> readContents()
    {
        List<ItemStackSnapshot> result = new ArrayList<>(AMOUNT_VARBITS.length);
        EnumComposition runeEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
        if (runeEnum == null) return result;

        for (int i = 0; i < AMOUNT_VARBITS.length; i++)
        {
            int amount = client.getVarbitValue(AMOUNT_VARBITS[i]);
            if (amount <= 0) continue;

            int runeType = client.getVarbitValue(RUNE_VARBITS[i]);
            if (runeType == 0) continue;

            int itemId = runeEnum.getIntValue(runeType);
            if (itemId <= 0) continue;
            String name = itemManager.getItemComposition(itemId).getName();
            if (name == null || name.trim().isEmpty()) continue;
            result.add(new ItemStackSnapshot(itemId, name, amount));
        }
        return result;
    }

    static boolean hasUsableRunePouch(InventorySnapshot inventory)
    {
        if (inventory == null || inventory.getItems() == null) return false;
        for (ItemStackSnapshot item : inventory.getItems())
        {
            if (item == null || item.getQuantity() <= 0 || item.getName() == null)
                continue;
            String name = item.getName().trim().toLowerCase(Locale.ROOT);

            // Fail closed on identity. Generic substring matching could treat a
            // future note/token/placeholder containing "rune pouch" as an
            // actually usable pouch and leak stale varbit runes into planning.
            if (name.equals("rune pouch")
                    || name.equals("divine rune pouch"))
            {
                return true;
            }
        }
        return false;
    }
}
