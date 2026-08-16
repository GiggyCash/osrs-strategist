package com.udderlywet.osrsstrategist;

public final class AgilityCourseDefinition
{
    private final String id;
    private final String displayName;
    private final int requiredLevel;
    private final int regionId;
    private final String requiredQuest;
    private final boolean wilderness;

    public AgilityCourseDefinition(
            String id,
            String displayName,
            int requiredLevel,
            int regionId,
            String requiredQuest,
            boolean wilderness)
    {
        this.id = id;
        this.displayName = displayName;
        this.requiredLevel = requiredLevel;
        this.regionId = regionId;
        this.requiredQuest = requiredQuest;
        this.wilderness = wilderness;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getRequiredLevel() { return requiredLevel; }
    public int getRegionId() { return regionId; }
    public String getRequiredQuest() { return requiredQuest; }
    public boolean isWilderness() { return wilderness; }

    public String observationKey()
    {
        return "region." + regionId;
    }
}
