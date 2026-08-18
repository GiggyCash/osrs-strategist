package com.udderlywet.osrsstrategist;

/** Generic non-skill work item that can compete for DO NEXT. */
public final class StrategyCandidate
{
    private final String id;
    private final String title;
    private final String reason;
    private final double score;
    private final RecommendationConfidence confidence;
    private final RecommendationGuidance guidance;
    private final CandidateSafetyEvidence safetyEvidence;

    public StrategyCandidate(
            String id,
            String title,
            String reason,
            double score,
            RecommendationConfidence confidence)
    {
        this(id, title, reason, score, confidence, null,
                CandidateSafetyEvidence.unknown());
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
                CandidateSafetyEvidence.unknown());
    }

    public StrategyCandidate(
            String id, String title, String reason, double score,
            RecommendationConfidence confidence, RecommendationGuidance guidance,
            CandidateSafetyEvidence safetyEvidence)
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
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getReason() { return reason; }
    public double getScore() { return score; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public RecommendationGuidance getGuidance() { return guidance; }
    public CandidateSafetyEvidence getSafetyEvidence() { return safetyEvidence; }

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
                safetyEvidence);
    }
}
