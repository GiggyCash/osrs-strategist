package com.udderlywet.osrsstrategist;

/** Immutable explanation and execution payload from the Slayer strategist. */
public final class SlayerDecisionResult
{
    private final SlayerAssignmentState assignmentState;
    private final SlayerTaskDecision decision;
    private final SlayerMasterProfile master;
    private final SlayerTaskStrategicProfile taskProfile;
    private final double score;
    private final RecommendationConfidence confidence;
    private final String reason;
    private final RecommendationGuidance guidance;
    private final String selectedAlternativeName;

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            RecommendationConfidence confidence, String reason,
            RecommendationGuidance guidance)
    {
        this(assignmentState, decision, master, taskProfile, score,
                confidence, reason, guidance, null);
    }

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            RecommendationConfidence confidence, String reason,
            RecommendationGuidance guidance, String selectedAlternativeName)
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
    }

    public SlayerAssignmentState getAssignmentState() { return assignmentState; }
    public SlayerTaskDecision getDecision() { return decision; }
    public SlayerMasterProfile getMaster() { return master; }
    public SlayerTaskStrategicProfile getTaskProfile() { return taskProfile; }
    public double getScore() { return score; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public String getReason() { return reason; }
    public RecommendationGuidance getGuidance() { return guidance; }
    public String getSelectedAlternativeName() { return selectedAlternativeName; }
}
