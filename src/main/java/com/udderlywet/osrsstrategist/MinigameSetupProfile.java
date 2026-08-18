package com.udderlywet.osrsstrategist;

/** Locally verifiable setup contract for a progression minigame. */
public final class MinigameSetupProfile
{
    private final String activityId;
    private final ItemRequirementExpression items;
    private final String location;
    private final String instructions;

    public MinigameSetupProfile(String activityId,
            ItemRequirementExpression items, String location,
            String instructions)
    {
        this.activityId = activityId;
        this.items = items;
        this.location = location;
        this.instructions = instructions;
    }

    public String getActivityId() { return activityId; }
    public ItemRequirementExpression getItems() { return items; }
    public String getLocation() { return location; }
    public String getInstructions() { return instructions; }
}
