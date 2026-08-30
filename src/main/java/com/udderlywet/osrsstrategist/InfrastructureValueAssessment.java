package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/** Typed result suitable for a future candidate strategic-value payload. */
public final class InfrastructureValueAssessment
{
    @Getter
    private final InfrastructureMilestoneDefinition milestone;
    @Getter
    private final InfrastructureMilestoneState state;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final StrategicPriority strategicValue;
    @Getter
    private final List<InfrastructureValueContribution> contributions;
    @Getter
    private final String reason;

    InfrastructureValueAssessment(InfrastructureMilestoneDefinition milestone,
            InfrastructureMilestoneState state,
            RecommendationConfidence confidence,
            StrategicPriority strategicValue,
            List<InfrastructureValueContribution> contributions,
            String reason)
    {
        this.milestone = milestone;
        this.state = state;
        this.confidence = confidence;
        this.strategicValue = strategicValue;
        this.contributions = Collections.unmodifiableList(
                new ArrayList<>(contributions));
        this.reason = reason == null ? "" : reason;
    }

    public boolean canRecommendAcquisition()
    {
        return state == InfrastructureMilestoneState.ACTIONABLE
                && confidence == RecommendationConfidence.VERIFIED;
    }
}
