package compass;

import java.util.*;

import lombok.Getter;

/**
 * Observed collection-log state plus explicitly proven long-form objectives.
 *
 * <p>Category counts are optional observed evidence. A missing category total is
 * unknown, not zero-complete. The named objective set is intentionally separate
 * from raw item IDs because some progression goals are multi-item/currency based.</p>
 */
@Getter
public final class CollectionLogSnapshot
{
    private final Set<Integer> obtainedItemIds;
    private final Set<String> completedObjectiveIds;
    private final Map<String, Integer> categoryCompleted;
    private final Map<String, Integer> categoryTotals;

    public CollectionLogSnapshot(Set<Integer> obtainedItemIds)
    {
        this(obtainedItemIds, Collections.emptySet(),
                Collections.emptyMap(), Collections.emptyMap());
    }

    public CollectionLogSnapshot(
            Set<Integer> obtainedItemIds,
            Set<String> completedObjectiveIds)
    {
        this(obtainedItemIds, completedObjectiveIds,
                Collections.emptyMap(), Collections.emptyMap());
    }

    public CollectionLogSnapshot(
            Set<Integer> obtainedItemIds,
            Set<String> completedObjectiveIds,
            Map<String, Integer> categoryCompleted,
            Map<String, Integer> categoryTotals)
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
        this.categoryCompleted = Collections.unmodifiableMap(
                categoryCompleted == null
                        ? new HashMap<>()
                        : new HashMap<>(categoryCompleted)
        );
        this.categoryTotals = Collections.unmodifiableMap(
                categoryTotals == null
                        ? new HashMap<>()
                        : new HashMap<>(categoryTotals)
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

    public int getCategoryCompleted(String category)
    {
        return categoryCompleted.getOrDefault(category, 0);
    }

    public int getCategoryTotal(String category)
    {
        return categoryTotals.getOrDefault(category, 0);
    }




}
