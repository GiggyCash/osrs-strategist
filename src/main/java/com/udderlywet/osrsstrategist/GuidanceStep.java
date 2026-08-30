package com.udderlywet.osrsstrategist;

import lombok.Getter;

public final class GuidanceStep
{
    @Getter
    private final String id;
    @Getter
    private final String label;
    @Getter
    private final String detail;
    @Getter
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

    public boolean isComplete() { return state == GuidanceStepState.COMPLETE; }
}
