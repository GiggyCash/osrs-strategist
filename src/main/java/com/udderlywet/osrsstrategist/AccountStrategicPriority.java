package com.udderlywet.osrsstrategist;

/** One explainable account-mode/state contribution. */
public final class AccountStrategicPriority
{
    private final AccountStrategicDimension dimension;
    private final StrategicPriority priority;
    private final CapabilityState capabilityState;
    private final RecommendationConfidence confidence;
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

    public AccountStrategicDimension getDimension() { return dimension; }
    public StrategicPriority getPriority() { return priority; }
    public CapabilityState getCapabilityState() { return capabilityState; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public String getReason() { return reason; }
}
