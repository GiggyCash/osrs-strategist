package com.udderlywet.osrsstrategist;

/** Result of evaluating a composable item requirement against observed state. */
public final class ItemRequirementResult
{
    private final RequirementState state;
    private final String action;

    public ItemRequirementResult(RequirementState state, String action)
    {
        this.state = state;
        this.action = action == null ? "" : action;
    }

    public RequirementState getState() { return state; }
    public String getAction() { return action; }
    public boolean isSatisfied() { return state == RequirementState.VERIFIED; }
}
