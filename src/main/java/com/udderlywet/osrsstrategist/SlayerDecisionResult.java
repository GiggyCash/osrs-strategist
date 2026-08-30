package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Immutable explanation and execution payload from the Slayer strategist. */
public final class SlayerDecisionResult
{
    @Getter
    private final SlayerAssignmentState assignmentState;
    @Getter
    private final SlayerTaskDecision decision;
    @Getter
    private final SlayerMasterProfile master;
    @Getter
    private final SlayerTaskStrategicProfile taskProfile;
    @Getter
    private final double score;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final String reason;
    @Getter
    private final RecommendationGuidance guidance;
    @Getter
    private final String selectedAlternativeName;
    @Getter
    private final SlayerReward recommendedReward;
    @Getter
    private final SlayerTaskOffer recommendedOffer;

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            RecommendationConfidence confidence, String reason,
            RecommendationGuidance guidance)
    {
        this(assignmentState, decision, master, taskProfile, score,
                confidence, reason, guidance, null, null, null);
    }

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            RecommendationConfidence confidence, String reason,
            RecommendationGuidance guidance, String selectedAlternativeName)
    {
        this(assignmentState, decision, master, taskProfile, score, confidence,
                reason, guidance, selectedAlternativeName, null, null);
    }

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            RecommendationConfidence confidence, String reason,
            RecommendationGuidance guidance, String selectedAlternativeName,
            SlayerReward recommendedReward)
    {
        this(assignmentState, decision, master, taskProfile, score, confidence,
                reason, guidance, selectedAlternativeName, recommendedReward,
                null);
    }

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            RecommendationConfidence confidence, String reason,
            RecommendationGuidance guidance, String selectedAlternativeName,
            SlayerReward recommendedReward, SlayerTaskOffer recommendedOffer)
    {
        this.assignmentState = assignmentState == null
                ? SlayerAssignmentState.UNKNOWN : assignmentState;
        this.decision = decision;
        this.master = master;
        this.taskProfile = taskProfile;
        this.score = score;
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED : confidence;
        this.reason = reason;
        this.guidance = guidance;
        this.selectedAlternativeName = selectedAlternativeName;
        this.recommendedReward = recommendedReward;
        this.recommendedOffer = recommendedOffer;
    }

}
