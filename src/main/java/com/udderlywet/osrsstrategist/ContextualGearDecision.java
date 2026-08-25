package com.udderlywet.osrsstrategist;

public final class ContextualGearDecision
{
    private final GearDecisionKind kind;
    private final String value;
    private final RecommendationConfidence confidence;

    ContextualGearDecision(GearDecisionKind kind, String value,
            RecommendationConfidence confidence)
    {
        this.kind = kind;
        this.value = value;
        this.confidence = confidence;
    }

    public GearDecisionKind getKind() { return kind; }
    public String getValue() { return value; }
    public RecommendationConfidence getConfidence() { return confidence; }
}
