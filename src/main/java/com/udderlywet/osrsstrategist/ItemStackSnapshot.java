package com.udderlywet.osrsstrategist;

import lombok.Getter;

public final class ItemStackSnapshot
{
    @Getter
    private final int itemId;
    @Getter
    private final String name;
    @Getter
    private final int quantity;
    private final int slotIndex;

    public ItemStackSnapshot(int itemId, String name, int quantity)
    {
        this(itemId, name, quantity, -1);
    }

    public ItemStackSnapshot(int itemId, String name, int quantity, int slotIndex)
    {
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
        this.slotIndex = slotIndex;
    }




    /** Container index when observed live, or -1 for persisted/synthetic data. */
    public int getSlotIndex()
    {
        return slotIndex;
    }
}
