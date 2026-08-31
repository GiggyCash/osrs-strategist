package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;

/**
 * Reads the item containers RuneLite can observe safely without automating any
 * gameplay.
 *
 * <p>The bank is cached only after RuneLite exposes the bank container. If the
 * account has not opened its bank during this client session, {@code readBank}
 * returns the last verified cache (or null). Compass therefore never treats
 * an unopened bank as an empty bank.</p>
 */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class LiveItemStateReader
{
    private final Client client;
    private final ItemManager itemManager;
    private ItemsState lastBankSnapshot;
    private ItemsState lastGroupStorageSnapshot;

    public ItemsState readInventory()
    {
        List<ItemState> items =
                readContainer(InventoryID.INV);

        return items == null
                ? null
                : new ItemsState(items, true);
    }

    public ItemsState readEquipment()
    {
        List<ItemState> items =
                readContainer(InventoryID.WORN);

        return items == null
                ? null
                : new ItemsState(items);
    }

    public ItemsState readBank()
    {
        List<ItemState> items =
                readContainer(InventoryID.BANK);

        if (items != null)
        {
            lastBankSnapshot = new ItemsState(
                    items,
                    System.currentTimeMillis()
            );
        }

        return lastBankSnapshot;
    }

    /** Shared storage is usable only after this character actually opens it. */
    public ItemsState readGroupStorage()
    {
        return lastGroupStorageSnapshot;
    }

    public void observeGroupStorage(ItemContainer container)
    {
        var items = snapshot(container);
        if (items != null)
            lastGroupStorageSnapshot = new ItemsState(
                    true, items, System.currentTimeMillis());
    }

    public void clearAccountCaches()
    {
        lastBankSnapshot = null;
        lastGroupStorageSnapshot = null;
    }

    private List<ItemState> readContainer(
            int inventoryId)
    {
        ItemContainer container =
                client.getItemContainer(inventoryId);

        if (container == null)
        {
            return null;
        }

        return snapshot(container);
    }

    private List<ItemState> snapshot(ItemContainer container)
    {
        if (container == null) return null;
        List<ItemState> result = new ArrayList<>();
        var containerItems = container.getItems();
        for (int slotIndex = 0; slotIndex < containerItems.length; slotIndex++)
        {
            var item = containerItems[slotIndex];
            if (item == null
                    || item.getId() < 0
                    || item.getQuantity() <= 0)
            {
                continue;
            }

            String name = itemManager
                    .getItemComposition(item.getId())
                    .getName();

            result.add(
                    new ItemState(
                            item.getId(),
                            name,
                            item.getQuantity(),
                            slotIndex
                    )
            );
        }

        return result;
    }
}
