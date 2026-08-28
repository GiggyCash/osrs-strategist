package com.udderlywet.osrsstrategist;

/** Property-driven value of one exact upgrade in one supplied context. */
public final class ContextualGearValueAssessment
{
    private final GearUpgradeValueState state;
    private final int scoreAdjustment;
    private final GearAcquisitionRoute acquisitionRoute;
    private final String evidence;

    ContextualGearValueAssessment(GearUpgradeValueState state,
            int scoreAdjustment, GearAcquisitionRoute acquisitionRoute,
            String evidence)
    {
        this.state = state;
        this.scoreAdjustment = scoreAdjustment;
        this.acquisitionRoute = acquisitionRoute;
        this.evidence = evidence;
    }

    public GearUpgradeValueState getState() { return state; }
    public int getScoreAdjustment() { return scoreAdjustment; }
    public GearAcquisitionRoute getAcquisitionRoute() { return acquisitionRoute; }
    public String getEvidence() { return evidence; }
}
