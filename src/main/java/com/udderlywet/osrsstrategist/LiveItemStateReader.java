package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
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
public class LiveItemStateReader
{
    private final Client client;
    private final ItemManager itemManager;
    private BankSnapshot lastBankSnapshot;

    @Inject
    public LiveItemStateReader(
            Client client,
            ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
    }

    public InventorySnapshot readInventory()
    {
        List<ItemStackSnapshot> items =
                readContainer(InventoryID.INV);

        return items == null
                ? null
                : new InventorySnapshot(items);
    }

    public EquipmentSnapshot readEquipment()
    {
        List<ItemStackSnapshot> items =
                readContainer(InventoryID.WORN);

        return items == null
                ? null
                : new EquipmentSnapshot(items);
    }

    public BankSnapshot readBank()
    {
        List<ItemStackSnapshot> items =
                readContainer(InventoryID.BANK);

        if (items != null)
        {
            lastBankSnapshot = new BankSnapshot(
                    items,
                    System.currentTimeMillis()
            );
        }

        return lastBankSnapshot;
    }

    public void clearAccountCaches()
    {
        lastBankSnapshot = null;
    }

    private List<ItemStackSnapshot> readContainer(
            int inventoryId)
    {
        ItemContainer container =
                client.getItemContainer(inventoryId);

        if (container == null)
        {
            return null;
        }

        List<ItemStackSnapshot> result = new ArrayList<>();

        Item[] containerItems = container.getItems();
        for (int slotIndex = 0; slotIndex < containerItems.length; slotIndex++)
        {
            Item item = containerItems[slotIndex];
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
                    new ItemStackSnapshot(
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
