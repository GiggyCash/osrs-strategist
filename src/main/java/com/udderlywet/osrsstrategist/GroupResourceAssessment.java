package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Bounded GIM resource value derived from one fresh storage observation. */
public final class GroupResourceAssessment
{
    @Getter
    private final GroupResourceState state;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final int observedSharedQuantity;
    @Getter
    private final int requiredQuantity;
    @Getter
    private final double duplicateGrindAvoidance;
    @Getter
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
