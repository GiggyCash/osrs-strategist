package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Exact material quantity resolved for one planned training segment. */
public final class ResolvedMethodInput
{
    @Getter
    private final String name;
    @Getter
    private final int itemId;
    @Getter
    private final int quantity;

    public ResolvedMethodInput(String name, int itemId, int quantity)
    {
        this.name = name;
        this.itemId = itemId;
        this.quantity = Math.max(0, quantity);
    }

}
