package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Small piece of verified game-data describing a Farming patch group.
 *
 * <p>Region IDs are used only as positive observation evidence. Quest
 * requirements are used to infer access before the patch has ever been visited.</p>
 */
public final class FarmingAccessDefinition
{
    private final String id;
    private final String displayName;
    private final Set<Integer> regionIds;
    private final String requiredQuest;
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

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Set<Integer> getRegionIds() { return regionIds; }
    public String getRequiredQuest() { return requiredQuest; }
    public boolean isHerbPatch() { return herbPatch; }

    public String observationKey()
    {
        return "farming.patch." + id;
    }
}
