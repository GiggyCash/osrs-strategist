package com.udderlywet.osrsstrategist;

/** Prominent, typed warning for an unusual player-visible dangerous action. */
public final class RecommendationRiskDisclosure
{
    private final String heading;
    private final String message;
    private final boolean acknowledgementRequired;

    public RecommendationRiskDisclosure(String heading, String message,
            boolean acknowledgementRequired)
    {
        this.heading = heading;
        this.message = message;
        this.acknowledgementRequired = acknowledgementRequired;
    }

    public static RecommendationRiskDisclosure deathStorage()
    {
        return new RecommendationRiskDisclosure("HIGH RISK",
                "This strategy uses death-based item storage. Incorrect execution or another unsafe death may permanently destroy stored items. Verify the exact retrieval rules before continuing. Use at your own risk; Gielinor Compass cannot recover or reimburse lost items.",
                true);
    }

    public String getHeading() { return heading; }
    public String getMessage() { return message; }
    public boolean isAcknowledgementRequired()
    {
        return acknowledgementRequired;
    }
}
