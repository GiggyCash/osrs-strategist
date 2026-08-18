package com.udderlywet.osrsstrategist;

/**
 * One ranked action the player could take next.
 *
 * <p>Current/target levels are structured fields instead of being buried in a
 * paragraph. That lets the sidebar stay compact while Details can still expose
 * the full reasoning.</p>
 */
public final class Recommendation
{
    private final String id;
    private final String title;
    private final String reason;
    private final double score;
    private final TrainingPlan trainingPlan;
    private final RecommendationConfidence confidence;
    private final int currentLevel;
    private final int targetLevel;
    private final RecommendationGuidance guidance;
    private final CandidateSafetyEvidence safetyEvidence;

    public Recommendation(
            String id,
            String title,
            String reason,
            double score)
    {
        this(
                id,
                title,
                reason,
                score,
                null,
                RecommendationConfidence.CHECK_NEEDED,
                0,
                0,
                null,
                CandidateSafetyEvidence.unknown()
        );
    }

    public Recommendation(
            String id,
            String title,
            String reason,
            double score,
            TrainingPlan trainingPlan,
            RecommendationConfidence confidence)
    {
        this(
                id,
                title,
                reason,
                score,
                trainingPlan,
                confidence,
                0,
                0,
                null,
                CandidateSafetyEvidence.unknown()
        );
    }

    public Recommendation(
            String id,
            String title,
            String reason,
            double score,
            TrainingPlan trainingPlan,
            RecommendationConfidence confidence,
            int currentLevel,
            int targetLevel)
    {
        this(
                id,
                title,
                reason,
                score,
                trainingPlan,
                confidence,
                currentLevel,
                targetLevel,
                null,
                CandidateSafetyEvidence.unknown()
        );
    }

    public Recommendation(
            String id,
            String title,
            String reason,
            double score,
            TrainingPlan trainingPlan,
            RecommendationConfidence confidence,
            int currentLevel,
            int targetLevel,
            RecommendationGuidance guidance)
    {
        this(id, title, reason, score, trainingPlan, confidence, currentLevel,
                targetLevel, guidance, CandidateSafetyEvidence.unknown());
    }

    public Recommendation(
            String id, String title, String reason, double score,
            TrainingPlan trainingPlan, RecommendationConfidence confidence,
            int currentLevel, int targetLevel, RecommendationGuidance guidance,
            CandidateSafetyEvidence safetyEvidence)
    {
        this.id = id;
        this.title = title;
        this.reason = reason;
        this.score = score;
        this.trainingPlan = trainingPlan;
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED
                : confidence;
        this.currentLevel = Math.max(0, currentLevel);
        this.targetLevel = Math.max(0, targetLevel);
        this.guidance = guidance;
        this.safetyEvidence = safetyEvidence == null
                ? CandidateSafetyEvidence.unknown() : safetyEvidence;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getReason() { return reason; }
    public double getScore() { return score; }
    public TrainingPlan getTrainingPlan() { return trainingPlan; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public int getCurrentLevel() { return currentLevel; }
    public int getTargetLevel() { return targetLevel; }
    public RecommendationGuidance getGuidance() { return guidance; }
    public CandidateSafetyEvidence getSafetyEvidence() { return safetyEvidence; }
}
