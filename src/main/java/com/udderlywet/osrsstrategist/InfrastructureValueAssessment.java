package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Typed result suitable for a future candidate strategic-value payload. */
public final class InfrastructureValueAssessment
{
    private final InfrastructureMilestoneDefinition milestone;
    private final InfrastructureMilestoneState state;
    private final RecommendationConfidence confidence;
    private final StrategicPriority strategicValue;
    private final List<InfrastructureValueContribution> contributions;
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

    public InfrastructureMilestoneDefinition getMilestone() { return milestone; }
    public InfrastructureMilestoneState getState() { return state; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public StrategicPriority getStrategicValue() { return strategicValue; }
    public List<InfrastructureValueContribution> getContributions()
    {
        return contributions;
    }
    public String getReason() { return reason; }
    public boolean canRecommendAcquisition()
    {
        return state == InfrastructureMilestoneState.ACTIONABLE
                && confidence == RecommendationConfidence.VERIFIED;
    }
}
