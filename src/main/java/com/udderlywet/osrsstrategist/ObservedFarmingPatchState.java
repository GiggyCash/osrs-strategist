package com.udderlywet.osrsstrategist;

public final class ObservedFarmingPatchState
{
    private final FarmingPatchCycleState state;
    private final long observedAtMillis;

    public ObservedFarmingPatchState(
            FarmingPatchCycleState state,
            long observedAtMillis)
    {
        this.state = state == null ? FarmingPatchCycleState.UNKNOWN : state;
        this.observedAtMillis = observedAtMillis;
    }

    public FarmingPatchCycleState getState() { return state; }
    public long getObservedAtMillis() { return observedAtMillis; }
}
