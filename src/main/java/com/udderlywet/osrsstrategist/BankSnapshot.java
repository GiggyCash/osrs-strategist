package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BankSnapshot
{
    private final List<ItemStackSnapshot> items;
    private final long capturedAtMillis;

    public BankSnapshot(
            List<ItemStackSnapshot> items,
            long capturedAtMillis)
    {
        this.items = Collections.unmodifiableList(
                new ArrayList<>(items)
        );
        this.capturedAtMillis = capturedAtMillis;
    }

    public List<ItemStackSnapshot> getItems()
    {
        return items;
    }

    public long getCapturedAtMillis()
    {
        return capturedAtMillis;
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
