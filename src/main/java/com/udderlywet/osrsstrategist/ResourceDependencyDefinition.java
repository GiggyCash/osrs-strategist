package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Verified self-source recipe/access route for one resource family. */
public final class ResourceDependencyDefinition
{
    @Getter
    private final int itemId;
    @Getter
    private final String itemName;
    @Getter
    private final String action;
    @Getter
    private final int opportunityCost;
    @Getter
    private final int outputQuantity;
    @Getter
    private final List<DependencyRequirement> prerequisites;

    public ResourceDependencyDefinition(int itemId, String action,
            int opportunityCost, List<DependencyRequirement> prerequisites)
    {
        this(itemId, null, action, opportunityCost, 1, prerequisites);
    }

    public ResourceDependencyDefinition(int itemId, String action,
            int opportunityCost, int outputQuantity,
            List<DependencyRequirement> prerequisites)
    {
        this(itemId, null, action, opportunityCost, outputQuantity, prerequisites);
    }

    public ResourceDependencyDefinition(int itemId, String itemName, String action,
            int opportunityCost, List<DependencyRequirement> prerequisites)
    {
        this(itemId, itemName, action, opportunityCost, 1, prerequisites);
    }

    public ResourceDependencyDefinition(int itemId, String itemName, String action,
            int opportunityCost, int outputQuantity,
            List<DependencyRequirement> prerequisites)
    {
        this.itemId = itemId;
        this.itemName = itemName == null ? null : itemName.trim();
        this.action = action;
        this.opportunityCost = Math.max(0, opportunityCost);
        this.outputQuantity = Math.max(1, outputQuantity);
        this.prerequisites = Collections.unmodifiableList(prerequisites == null
                ? new ArrayList<>() : new ArrayList<>(prerequisites));
    }

}
