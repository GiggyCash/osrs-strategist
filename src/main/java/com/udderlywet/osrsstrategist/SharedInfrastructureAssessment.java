package com.udderlywet.osrsstrategist;

/** Explicit boundary for group capabilities RuneLite does not observe. */
public final class SharedInfrastructureAssessment
{
    private final CapabilityState state;
    private final RecommendationConfidence confidence;
    private final String reason;

    SharedInfrastructureAssessment(CapabilityState state,
            RecommendationConfidence confidence, String reason)
    {
        this.state = state;
        this.confidence = confidence;
        this.reason = reason;
    }

    public CapabilityState getState() { return state; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public String getReason() { return reason; }
}
