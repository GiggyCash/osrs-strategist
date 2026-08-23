package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Verified self-source recipe/access route for one resource family. */
public final class ResourceDependencyDefinition
{
    private final int itemId;
    private final String itemName;
    private final String action;
    private final int opportunityCost;
    private final int outputQuantity;
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

    public int getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getAction() { return action; }
    public int getOpportunityCost() { return opportunityCost; }
    public int getOutputQuantity() { return outputQuantity; }
    public List<DependencyRequirement> getPrerequisites() { return prerequisites; }
}
