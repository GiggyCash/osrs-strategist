package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Typed result suitable for a future candidate strategic-value payload. */
@Getter
public final class InfrastructureValueAssessment
{
    private final InfrastructureMilestone milestone;
    private final InfrastructureMilestoneState state;
    private final Confidence confidence;
    private final StrategicPriority strategicValue;
    private final List<InfrastructureValueContribution> contributions;
    private final String reason;

    InfrastructureValueAssessment(InfrastructureMilestone milestone,
            InfrastructureMilestoneState state,
            Confidence confidence,
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
                && confidence == Confidence.VERIFIED;
    }
}
