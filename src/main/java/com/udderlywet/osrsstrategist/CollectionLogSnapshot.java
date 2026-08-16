package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Observed collection-log state plus explicitly proven long-form objectives.
 *
 * <p>The named objective set is intentionally separate from raw item IDs. Some
 * progression goals are multi-item or currency based, and the plugin must not
 * infer completion from an incomplete collection-log snapshot.</p>
 */
public final class CollectionLogSnapshot
{
    private final Set<Integer> obtainedItemIds;
    private final Set<String> completedObjectiveIds;

    public CollectionLogSnapshot(Set<Integer> obtainedItemIds)
    {
        this(obtainedItemIds, Collections.emptySet());
    }

    public CollectionLogSnapshot(
            Set<Integer> obtainedItemIds,
            Set<String> completedObjectiveIds)
    {
        this.obtainedItemIds = Collections.unmodifiableSet(
                obtainedItemIds == null
                        ? new HashSet<>()
                        : new HashSet<>(obtainedItemIds)
        );
        this.completedObjectiveIds = Collections.unmodifiableSet(
                completedObjectiveIds == null
                        ? new HashSet<>()
                        : new HashSet<>(completedObjectiveIds)
        );
    }

    public boolean hasItem(int itemId)
    {
        return obtainedItemIds.contains(itemId);
    }

    public boolean isObjectiveComplete(String objectiveId)
    {
        return objectiveId != null
                && completedObjectiveIds.contains(objectiveId);
    }

    public int obtainedCount()
    {
        return obtainedItemIds.size();
    }

    public Set<Integer> getObtainedItemIds()
    {
        return obtainedItemIds;
    }

    public Set<String> getCompletedObjectiveIds()
    {
        return completedObjectiveIds;
    }
}
