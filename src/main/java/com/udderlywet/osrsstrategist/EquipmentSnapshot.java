package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable view of items currently equipped by the player. */
public final class EquipmentSnapshot
{
    private final List<ItemStackSnapshot> equippedItems;

    public EquipmentSnapshot(List<ItemStackSnapshot> equippedItems)
    {
        this.equippedItems = Collections.unmodifiableList(
                new ArrayList<>(equippedItems)
        );
    }

    public List<ItemStackSnapshot> getEquippedItems()
    {
        return equippedItems;
    }

    /**
     * Resource checks must include equipment. This matters for things such as
     * Runecraft tiaras, worn graceful pieces, Slayer equipment, and future gear
     * requirements where an item being worn is just as usable as one sitting in
     * the inventory.
     */
    public int quantityOf(int itemId)
    {
        int total = 0;
        for (ItemStackSnapshot item : equippedItems)
        {
            if (item.getItemId() == itemId) total += item.getQuantity();
        }
        return total;
    }
}
