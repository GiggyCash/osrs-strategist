package com.udderlywet.osrsstrategist;

import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GroupStorageFreshnessTest
{
    @Test
    public void recentObservationCountsAndStaleObservationFailsClosed()
    {
        long now = System.currentTimeMillis();
        GroupStorageSnapshot recent = new GroupStorageSnapshot(true,
                Collections.singletonList(new ItemStackSnapshot(
                        1, "Shared item", 1)), now);
        GroupStorageSnapshot stale = new GroupStorageSnapshot(true,
                Collections.singletonList(new ItemStackSnapshot(
                        1, "Shared item", 1)),
                now - GroupStorageSnapshot.FRESH_FOR_MILLIS - 1L);

        assertTrue(recent.isObserved());
        assertTrue(recent.containsItem(1));
        assertFalse(stale.isObserved());
        assertFalse(stale.containsItem(1));
        assertFalse(GroupStorageSnapshot.unknown().isObserved());
    }

    @Test
    public void futureDatedObservationFailsClosed()
    {
        GroupStorageSnapshot future = new GroupStorageSnapshot(true,
                Collections.emptyList(),
                System.currentTimeMillis() + 60_000L);
        assertFalse(future.isObserved());
    }
}
