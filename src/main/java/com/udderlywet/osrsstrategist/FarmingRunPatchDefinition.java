package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

public final class FarmingRunPatchDefinition
{
    @Getter
    private final String id;
    @Getter
    private final String displayName;
    @Getter
    private final FarmingPatchKind kind;
    @Getter
    private final int minimumLevel;
    @Getter
    private final Set<Integer> regionIds;
    @Getter
    private final int varbitId;
    @Getter
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


    public boolean matchesRegion(int regionId)
    {
        return regionIds.contains(regionId);
    }

    public String observationKey()
    {
        return "farm-run." + id;
    }
}
