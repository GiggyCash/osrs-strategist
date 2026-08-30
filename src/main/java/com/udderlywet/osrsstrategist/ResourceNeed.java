package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * One item requirement for an activity, training method, gear upgrade, or
 * preparation checklist.
 */
public final class ResourceNeed
{
    @Getter
    private final int itemId;
    @Getter
    private final String itemName;
    @Getter
    private final int quantity;

    public ResourceNeed(int itemId, String itemName, int quantity)
    {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = Math.max(1, quantity);
    }

}
