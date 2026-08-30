package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Generic non-skill work item that can compete for DO NEXT. */
public final class StrategyCandidate
{
    @Getter
    private final String id;
    @Getter
    private final String title;
    @Getter
    private final String reason;
    @Getter
    private final double score;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final RecommendationGuidance guidance;
    @Getter
    private final CandidateSafetyEvidence safetyEvidence;
    @Getter
    private final RecommendationStrategicValue strategicValue;

    public StrategyCandidate(
            String id,
            String title,
            String reason,
            double score,
            RecommendationConfidence confidence)
    {
        this(id, title, reason, score, confidence, null,
                CandidateSafetyEvidence.unknown(),
                RecommendationStrategicValue.neutral());
    }

    public StrategyCandidate(
            String id,
            String title,
            String reason,
            double score,
            RecommendationConfidence confidence,
            RecommendationGuidance guidance)
    {
        this(id, title, reason, score, confidence, guidance,
                CandidateSafetyEvidence.unknown(),
                RecommendationStrategicValue.neutral());
    }

    public StrategyCandidate(
            String id, String title, String reason, double score,
            RecommendationConfidence confidence, RecommendationGuidance guidance,
            CandidateSafetyEvidence safetyEvidence)
    {
        this(id, title, reason, score, confidence, guidance, safetyEvidence,
                RecommendationStrategicValue.neutral());
    }

    public StrategyCandidate(
            String id, String title, String reason, double score,
            RecommendationConfidence confidence, RecommendationGuidance guidance,
            CandidateSafetyEvidence safetyEvidence,
            RecommendationStrategicValue strategicValue)
    {
        this.id = id;
        this.title = title;
        this.reason = reason;
        this.score = score;
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED
                : confidence;
        this.guidance = guidance;
        this.safetyEvidence = safetyEvidence == null
                ? CandidateSafetyEvidence.unknown() : safetyEvidence;
        this.strategicValue = strategicValue == null
                ? RecommendationStrategicValue.neutral() : strategicValue;
    }


    public Recommendation toRecommendation()
    {
        return new Recommendation(
                id,
                title,
                reason,
                score,
                null,
                confidence,
                0,
                0,
                guidance,
                safetyEvidence).withStrategicValue(strategicValue);
    }
}
