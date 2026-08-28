package com.udderlywet.osrsstrategist;

/** Travel evidence and bounded value for a selected concrete method location. */
public final class TravelAwareMethodAssessment
{
    private final MethodLocationOption location;
    private final int travelBurden;
    private final int scoreAdjustment;
    private final boolean verifiedRouteUsed;
    private final String evidence;

    TravelAwareMethodAssessment(MethodLocationOption location,
            int travelBurden, int scoreAdjustment, boolean verifiedRouteUsed,
            String evidence)
    {
        this.location = location;
        this.travelBurden = travelBurden;
        this.scoreAdjustment = scoreAdjustment;
        this.verifiedRouteUsed = verifiedRouteUsed;
        this.evidence = evidence;
    }

    public MethodLocationOption getLocation() { return location; }
    public int getTravelBurden() { return travelBurden; }
    public int getScoreAdjustment() { return scoreAdjustment; }
    public boolean isVerifiedRouteUsed() { return verifiedRouteUsed; }
    public String getEvidence() { return evidence; }
}
