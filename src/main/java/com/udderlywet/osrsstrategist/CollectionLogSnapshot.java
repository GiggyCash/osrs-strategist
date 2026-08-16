package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class CollectionLogSnapshot
{
    private final Set<Integer> obtainedItemIds;

    public CollectionLogSnapshot(Set<Integer> obtainedItemIds)
    {
        this.obtainedItemIds = Collections.unmodifiableSet(
                new HashSet<>(obtainedItemIds)
        );
    }

    public boolean hasItem(int itemId)
    {
        return obtainedItemIds.contains(itemId);
    }

    public int obtainedCount()
    {
        return obtainedItemIds.size();
    }
}
