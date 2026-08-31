package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * One item requirement for an activity, training method, gear upgrade, or
 * preparation checklist.
 */
@Getter
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

}
