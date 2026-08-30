package com.udderlywet.osrsstrategist;

import lombok.Getter;

public final class ObservedFarmingPatchState
{
    @Getter
    private final FarmingPatchCycleState state;
    @Getter
    private final long observedAtMillis;

    public ObservedFarmingPatchState(
            FarmingPatchCycleState state,
            long observedAtMillis)
    {
        this.state = state == null ? FarmingPatchCycleState.UNKNOWN : state;
        this.observedAtMillis = observedAtMillis;
    }

}
