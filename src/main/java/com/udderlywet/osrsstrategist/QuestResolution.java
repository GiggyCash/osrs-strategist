package com.udderlywet.osrsstrategist;

/** Actionability result for one fully identified quest. */
public final class QuestResolution
{
    private final RecommendationConfidence confidence;
    private final RecommendationGuidance guidance;
    private final String reason;

    public QuestResolution(RecommendationConfidence confidence,
            RecommendationGuidance guidance, String reason)
    {
        this.confidence = confidence;
        this.guidance = guidance;
        this.reason = reason;
    }

    public RecommendationConfidence getConfidence() { return confidence; }
    public RecommendationGuidance getGuidance() { return guidance; }
    public String getReason() { return reason; }
}
