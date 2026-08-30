package com.udderlywet.osrsstrategist;

/** Locally verifiable setup contract for a progression minigame. */
public final class MinigameSetupProfile
{
    private final String activityId;
    private final ItemRequirementExpression items;
    private final String location;
    private final String supplies;
    private final String instructions;

    public MinigameSetupProfile(String activityId,
            ItemRequirementExpression items, String location,
            String supplies, String instructions)
    {
        this.activityId = activityId;
        this.items = items;
        this.location = location;
        this.supplies = supplies;
        this.instructions = instructions;
    }

    public String getActivityId() { return activityId; }
    public ItemRequirementExpression getItems() { return items; }
    public String getLocation() { return location; }
    public String getSupplies() { return supplies; }
    public String getInstructions() { return instructions; }
}
