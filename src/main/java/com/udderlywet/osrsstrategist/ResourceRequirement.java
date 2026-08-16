package com.udderlywet.osrsstrategist;

import java.util.Arrays;

/**
 * A reusable observed-resource requirement. itemIds are alternatives: quantities
 * across every listed ID are summed toward the requirement.
 */
public final class ResourceRequirement
{
    private final String id;
    private final String label;
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

    public String getId() { return id; }
    public String getLabel() { return label; }
    public int getRequiredQuantity() { return requiredQuantity; }
    public int[] getItemIds() { return Arrays.copyOf(itemIds, itemIds.length); }
}
