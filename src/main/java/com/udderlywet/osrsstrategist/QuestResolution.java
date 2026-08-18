package com.udderlywet.osrsstrategist;

/** Actionability result for one fully identified quest. */
public final class QuestResolution
{
    private final RecommendationConfidence confidence;
    private final RecommendationGuidance guidance;
    private final String reason;
    private final CandidateSafetyEvidence safetyEvidence;

    public QuestResolution(RecommendationConfidence confidence,
            RecommendationGuidance guidance, String reason)
    {
        this(confidence, guidance, reason, CandidateSafetyEvidence.unknown());
    }

    public QuestResolution(RecommendationConfidence confidence,
            RecommendationGuidance guidance, String reason,
            CandidateSafetyEvidence safetyEvidence)
    {
        this.confidence = confidence;
        this.guidance = guidance;
        this.reason = reason;
        this.safetyEvidence = safetyEvidence;
    }

    public RecommendationConfidence getConfidence() { return confidence; }
    public RecommendationGuidance getGuidance() { return guidance; }
    public String getReason() { return reason; }
    public CandidateSafetyEvidence getSafetyEvidence() { return safetyEvidence; }
}
