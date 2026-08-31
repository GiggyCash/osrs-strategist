package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/**
 * Small piece of verified game-data describing a Farming patch group.
 *
 * <p>Region IDs are used only as positive observation evidence. Quest
 * requirements are used to infer access before the patch has ever been visited.</p>
 */
public final class FarmingAccessDefinition
{
    @Getter
    private final String id;
    @Getter
    private final String displayName;
    @Getter
    private final Set<Integer> regionIds;
    @Getter
    private final String requiredQuest;
    @Getter
    private final boolean herbPatch;

    public FarmingAccessDefinition(
            String id,
            String displayName,
            Set<Integer> regionIds,
            String requiredQuest,
            boolean herbPatch)
    {
        this.id = id;
        this.displayName = displayName;
        this.regionIds = Collections.unmodifiableSet(
                new HashSet<>(regionIds)
        );
        this.requiredQuest = requiredQuest;
        this.herbPatch = herbPatch;
    }


    public String observationKey()
    {
        return "farming.patch." + id;
    }
}
