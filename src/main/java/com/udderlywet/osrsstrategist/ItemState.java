package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One observed item stack; slot is -1 for persisted or synthetic evidence. */
@Getter
public final class ItemState
{
    private final int itemId;
    private final String name;
    private final int quantity;
    private final int slotIndex;

    public ItemState(int itemId, String name, int quantity)
    {
        this(itemId, name, quantity, -1);
    }

    public ItemState(int itemId, String name, int quantity, int slotIndex)
    {
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
        this.slotIndex = slotIndex;
    }
}
