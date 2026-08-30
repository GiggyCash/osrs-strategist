package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Actionability result for one fully identified quest. */
public final class QuestResolution
{
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final RecommendationGuidance guidance;
    @Getter
    private final String reason;
    @Getter
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
