package compass;

import java.util.Arrays;

import lombok.Getter;

/**
 * A reusable observed-resource requirement. itemIds are alternatives: quantities
 * across every listed ID are summed toward the requirement.
 */
public final class ResourceRequirement
{
    @Getter
    final String id;
    @Getter
    private final String label;
    @Getter
    private final int requiredQuantity;
    private final int[] itemIds;

    public ResourceRequirement(
            String id,
            String label,
            int requiredQuantity,
            int... itemIds)
    {
        this.id = id;
        this.label = label;
        this.requiredQuantity = Math.max(1, requiredQuantity);
        this.itemIds = itemIds == null ? new int[0] : Arrays.copyOf(itemIds, itemIds.length);
    }

    public int[] getItemIds() { return Arrays.copyOf(itemIds, itemIds.length); }
}
