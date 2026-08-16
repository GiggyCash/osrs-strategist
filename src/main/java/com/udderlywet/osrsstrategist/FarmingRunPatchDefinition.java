package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class FarmingRunPatchDefinition
{
    private final String id;
    private final String displayName;
    private final FarmingPatchKind kind;
    private final int minimumLevel;
    private final Set<Integer> regionIds;
    private final int varbitId;
    private final String requiredQuest;

    public FarmingRunPatchDefinition(
            String id,
            String displayName,
            FarmingPatchKind kind,
            int minimumLevel,
            Set<Integer> regionIds,
            int varbitId,
            String requiredQuest)
    {
        this.id = id;
        this.displayName = displayName;
        this.kind = kind;
        this.minimumLevel = minimumLevel;
        this.regionIds = Collections.unmodifiableSet(new HashSet<>(regionIds));
        this.varbitId = varbitId;
        this.requiredQuest = requiredQuest;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public FarmingPatchKind getKind() { return kind; }
    public int getMinimumLevel() { return minimumLevel; }
    public Set<Integer> getRegionIds() { return regionIds; }
    public int getVarbitId() { return varbitId; }
    public String getRequiredQuest() { return requiredQuest; }

    public boolean matchesRegion(int regionId)
    {
        return regionIds.contains(regionId);
    }

    public String observationKey()
    {
        return "farm-run." + id;
    }
}
