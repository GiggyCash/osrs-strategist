package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InventorySnapshot
{
    private final List<ItemStackSnapshot> items;

    public InventorySnapshot(List<ItemStackSnapshot> items)
    {
        this.items = Collections.unmodifiableList(
                new ArrayList<>(items)
        );
    }

    public List<ItemStackSnapshot> getItems()
    {
        return items;
    }

    public int quantityOf(int itemId)
    {
        int total = 0;

        for (ItemStackSnapshot item : items)
        {
            if (item.getItemId() == itemId)
            {
                total += item.getQuantity();
            }
        }

        return total;
    }
}
