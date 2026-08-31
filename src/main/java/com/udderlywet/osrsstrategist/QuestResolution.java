package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Actionability result for one fully identified quest. */
@Getter
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

}
