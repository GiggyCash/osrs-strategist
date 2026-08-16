package com.udderlywet.osrsstrategist;

public final class GuidanceStep
{
    private final String id;
    private final String label;
    private final String detail;
    private final GuidanceStepState state;

    public GuidanceStep(
            String id,
            String label,
            String detail,
            GuidanceStepState state)
    {
        this.id = id;
        this.label = label;
        this.detail = detail;
        this.state = state == null ? GuidanceStepState.CHECK_NEEDED : state;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public String getDetail() { return detail; }
    public GuidanceStepState getState() { return state; }
    public boolean isComplete() { return state == GuidanceStepState.COMPLETE; }
}
