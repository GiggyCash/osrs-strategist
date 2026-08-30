package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InventorySnapshot
{
    private final List<ItemStackSnapshot> items;
    private final boolean completeSlotObservation;

    public InventorySnapshot(List<ItemStackSnapshot> items)
    {
        this(items, hasLiveSlotEvidence(items));
    }

    public InventorySnapshot(List<ItemStackSnapshot> items,
            boolean completeSlotObservation)
    {
        this.items = Collections.unmodifiableList(
                new ArrayList<>(items)
        );
        this.completeSlotObservation = completeSlotObservation;
    }

    public List<ItemStackSnapshot> getItems()
    {
        return items;
    }

    public int quantityOf(int itemId)
    {
        int total = 0;

        for (ItemStackSnapshot item : items)
        {
            if (item.getItemId() == itemId)
            {
                total += item.getQuantity();
            }
        }

        return total;
    }

    public boolean hasCompleteSlotObservation()
    {
        return completeSlotObservation;
    }

    private static boolean hasLiveSlotEvidence(List<ItemStackSnapshot> items)
    {
        if (items == null) return false;
        for (ItemStackSnapshot item : items)
            if (item != null && item.getSlotIndex() >= 0) return true;
        return false;
    }
}
