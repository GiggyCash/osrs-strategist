package com.udderlywet.osrsstrategist;

/** Exact material quantity resolved for one planned training segment. */
public final class ResolvedMethodInput
{
    private final String name;
    private final int itemId;
    private final int quantity;

    public ResolvedMethodInput(String name, int itemId, int quantity)
    {
        this.name = name;
        this.itemId = itemId;
        this.quantity = Math.max(0, quantity);
    }

    public String getName() { return name; }
    public int getItemId() { return itemId; }
    public int getQuantity() { return quantity; }
}
