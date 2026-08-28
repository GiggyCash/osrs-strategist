package com.udderlywet.osrsstrategist;

/** Bounded GIM resource value derived from one fresh storage observation. */
public final class GroupResourceAssessment
{
    private final GroupResourceState state;
    private final RecommendationConfidence confidence;
    private final int observedSharedQuantity;
    private final int requiredQuantity;
    private final double duplicateGrindAvoidance;
    private final String reason;

    GroupResourceAssessment(GroupResourceState state,
            RecommendationConfidence confidence, int observedSharedQuantity,
            int requiredQuantity, double duplicateGrindAvoidance, String reason)
    {
        this.state = state;
        this.confidence = confidence;
        this.observedSharedQuantity = Math.max(0, observedSharedQuantity);
        this.requiredQuantity = Math.max(1, requiredQuantity);
        this.duplicateGrindAvoidance = Math.max(0.0,
                Math.min(1.0, duplicateGrindAvoidance));
        this.reason = reason == null ? "" : reason;
    }

    public GroupResourceState getState() { return state; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public int getObservedSharedQuantity() { return observedSharedQuantity; }
    public int getRequiredQuantity() { return requiredQuantity; }
    public double getDuplicateGrindAvoidance() { return duplicateGrindAvoidance; }
    public String getReason() { return reason; }
    public boolean satisfiesNeed()
    {
        return state == GroupResourceState.SHARED_STOCK_SATISFIES_NEED;
    }

    public RecommendationStrategicValue strategicValue(String evidenceId)
    {
        if (confidence != RecommendationConfidence.VERIFIED
                || duplicateGrindAvoidance <= 0.0)
            return RecommendationStrategicValue.neutral();
        return RecommendationStrategicValue.builder()
                .accountModeFit(duplicateGrindAvoidance * 0.6)
                .resourceFit(duplicateGrindAvoidance)
                .evidence(evidenceId)
                .build();
    }
}
