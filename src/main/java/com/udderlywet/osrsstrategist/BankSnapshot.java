package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

@Getter
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
