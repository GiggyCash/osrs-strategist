package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Immutable explanation and execution payload from the Slayer strategist. */
@Getter
public final class SlayerDecisionResult
{
    private final SlayerAssignmentState assignmentState;
    private final SlayerTaskDecision decision;
    private final SlayerMasterProfile master;
    private final SlayerTaskStrategicProfile taskProfile;
    private final double score;
    private final Confidence confidence;
    private final String reason;
    private final Guidance guidance;
    private final String selectedAlternativeName;
    private final SlayerReward recommendedReward;
    private final SlayerTaskOffer recommendedOffer;

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            Confidence confidence, String reason,
            Guidance guidance)
    {
        this(assignmentState, decision, master, taskProfile, score,
                confidence, reason, guidance, null, null, null);
    }

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            Confidence confidence, String reason,
            Guidance guidance, String selectedAlternativeName)
    {
        this(assignmentState, decision, master, taskProfile, score, confidence,
                reason, guidance, selectedAlternativeName, null, null);
    }

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            Confidence confidence, String reason,
            Guidance guidance, String selectedAlternativeName,
            SlayerReward recommendedReward)
    {
        this(assignmentState, decision, master, taskProfile, score, confidence,
                reason, guidance, selectedAlternativeName, recommendedReward,
                null);
    }

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            Confidence confidence, String reason,
            Guidance guidance, String selectedAlternativeName,
            SlayerReward recommendedReward, SlayerTaskOffer recommendedOffer)
    {
        this.assignmentState = assignmentState == null
                ? SlayerAssignmentState.UNKNOWN : assignmentState;
        this.decision = decision;
        this.master = master;
        this.taskProfile = taskProfile;
        this.score = score;
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED : confidence;
        this.reason = reason;
        this.guidance = guidance;
        this.selectedAlternativeName = selectedAlternativeName;
        this.recommendedReward = recommendedReward;
        this.recommendedOffer = recommendedOffer;
    }

}
