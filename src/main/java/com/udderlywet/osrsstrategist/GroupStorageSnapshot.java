package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private final boolean observed;
    private final List<ItemStackSnapshot> items;

    public GroupStorageSnapshot(
            boolean observed,
            List<ItemStackSnapshot> items)
    {
        this.observed = observed;
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
        return observed;
    }

    public List<ItemStackSnapshot> getItems()
    {
        return items;
    }

    public boolean containsItem(int itemId)
    {
        if (!observed)
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
