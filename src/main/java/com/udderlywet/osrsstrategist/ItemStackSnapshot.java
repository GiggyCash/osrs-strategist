package com.udderlywet.osrsstrategist;

public final class ItemStackSnapshot
{
    private final int itemId;
    private final String name;
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

    public int getItemId()
    {
        return itemId;
    }

    public String getName()
    {
        return name;
    }

    public int getQuantity()
    {
        return quantity;
    }

    /** Container index when observed live, or -1 for persisted/synthetic data. */
    public int getSlotIndex()
    {
        return slotIndex;
    }
}
