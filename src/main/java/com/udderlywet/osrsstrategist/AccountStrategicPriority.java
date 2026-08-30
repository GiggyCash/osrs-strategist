package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One explainable account-mode/state contribution. */
public final class AccountStrategicPriority
{
    @Getter
    private final AccountStrategicDimension dimension;
    @Getter
    private final StrategicPriority priority;
    @Getter
    private final CapabilityState capabilityState;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final String reason;

    public AccountStrategicPriority(
            AccountStrategicDimension dimension,
            StrategicPriority priority,
            CapabilityState capabilityState,
            RecommendationConfidence confidence,
            String reason)
    {
        if (dimension == null) throw new IllegalArgumentException("dimension");
        this.dimension = dimension;
        this.priority = priority == null ? StrategicPriority.NONE : priority;
        this.capabilityState = capabilityState == null
                ? CapabilityState.UNKNOWN : capabilityState;
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED : confidence;
        this.reason = reason == null ? "" : reason;
    }

}
