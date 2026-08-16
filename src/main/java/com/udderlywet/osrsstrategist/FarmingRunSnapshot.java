package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class FarmingRunSnapshot
{
    private final Map<String, ObservedFarmingPatchState> states;

    public FarmingRunSnapshot(Map<String, ObservedFarmingPatchState> states)
    {
        this.states = Collections.unmodifiableMap(
                states == null ? new HashMap<>() : new HashMap<>(states));
    }

    public static FarmingRunSnapshot empty()
    {
        return new FarmingRunSnapshot(Collections.emptyMap());
    }

    public ObservedFarmingPatchState stateOf(String patchId)
    {
        return states.get(patchId);
    }

    public Map<String, ObservedFarmingPatchState> getStates()
    {
        return states;
    }
}
