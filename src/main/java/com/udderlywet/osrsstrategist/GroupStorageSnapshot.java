package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/**
 * Latest Group Storage observation for a GIM account.
 *
 * <p>The observed flag is intentional. An empty observed storage is different
 * from a storage we have never inspected. Compass should only count group
 * items when this snapshot is observed and the user's Use Group Storage option
 * is enabled.</p>
 */
public final class GroupStorageSnapshot
{
    static final long FRESH_FOR_MILLIS = 5L * 60L * 1000L;
    private final boolean observed;
    @Getter
    private final List<ItemStackSnapshot> items;
    @Getter
    private final long observedAtMillis;

    public GroupStorageSnapshot(
            boolean observed,
            List<ItemStackSnapshot> items)
    {
        this(observed, items, observed ? System.currentTimeMillis() : 0L);
    }

    public GroupStorageSnapshot(
            boolean observed,
            List<ItemStackSnapshot> items,
            long observedAtMillis)
    {
        this.observed = observed;
        this.observedAtMillis = Math.max(0L, observedAtMillis);
        this.items = Collections.unmodifiableList(
                items == null
                        ? new ArrayList<>()
                        : new ArrayList<>(items)
        );
    }

    public static GroupStorageSnapshot unknown()
    {
        return new GroupStorageSnapshot(
                false,
                Collections.emptyList()
        );
    }

    public boolean isObserved()
    {
        long age = System.currentTimeMillis() - observedAtMillis;
        return observed && observedAtMillis > 0L
                && age >= 0L && age <= FRESH_FOR_MILLIS;
    }



    public boolean containsItem(int itemId)
    {
        if (!isObserved())
        {
            return false;
        }

        for (ItemStackSnapshot item : items)
        {
            if (item.getItemId() == itemId
                    && item.getQuantity() > 0)
            {
                return true;
            }
        }

        return false;
    }
}
