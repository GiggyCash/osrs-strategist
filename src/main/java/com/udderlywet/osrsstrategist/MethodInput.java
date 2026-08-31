package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Exact material quantity resolved for one planned training segment. */
@Getter
public final class MethodInput
{
    private final String name;
    private final int itemId;
    private final int quantity;

    public MethodInput(String name, int itemId, int quantity)
    {
        this.name = name;
        this.itemId = itemId;
        this.quantity = Math.max(0, quantity);
    }

}
