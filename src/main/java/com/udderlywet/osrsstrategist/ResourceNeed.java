package com.udderlywet.osrsstrategist;

/**
 * One item requirement for an activity, training method, gear upgrade, or
 * preparation checklist.
 */
public final class ResourceNeed
{
    private final int itemId;
    private final String itemName;
    private final int quantity;

    public ResourceNeed(int itemId, String itemName, int quantity)
    {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = Math.max(1, quantity);
    }

    public int getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public int getQuantity() { return quantity; }
}
