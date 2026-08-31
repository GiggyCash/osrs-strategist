package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * One ranked action the player could take next.
 *
 * <p>Current/target levels are structured fields instead of being buried in a
 * paragraph. That lets the sidebar stay compact while Details can still expose
 * the full reasoning.</p>
 */
@Getter
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
    private final GoalDependencyProvenance goalProvenance;
    private final RecommendationStrategicValue strategicValue;

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

    /** Non-skill candidate form used by activity providers. */
    public Recommendation(String id, String title, String reason, double score,
            RecommendationConfidence confidence)
    {
        this(id, title, reason, score, confidence, null,
                CandidateSafetyEvidence.unknown(),
                RecommendationStrategicValue.neutral());
    }

    public Recommendation(String id, String title, String reason, double score,
            RecommendationConfidence confidence, RecommendationGuidance guidance)
    {
        this(id, title, reason, score, confidence, guidance,
                CandidateSafetyEvidence.unknown(),
                RecommendationStrategicValue.neutral());
    }

    public Recommendation(String id, String title, String reason, double score,
            RecommendationConfidence confidence, RecommendationGuidance guidance,
            CandidateSafetyEvidence safetyEvidence)
    {
        this(id, title, reason, score, confidence, guidance, safetyEvidence,
                RecommendationStrategicValue.neutral());
    }

    public Recommendation(String id, String title, String reason, double score,
            RecommendationConfidence confidence, RecommendationGuidance guidance,
            CandidateSafetyEvidence safetyEvidence,
            RecommendationStrategicValue strategicValue)
    {
        this(id, title, reason, score, null, confidence, 0, 0, guidance,
                safetyEvidence, null, strategicValue);
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
        this(id, title, reason, score, trainingPlan, confidence, currentLevel,
                targetLevel, guidance, safetyEvidence, null,
                RecommendationStrategicValue.neutral());
    }

    private Recommendation(
            String id, String title, String reason, double score,
            TrainingPlan trainingPlan, RecommendationConfidence confidence,
            int currentLevel, int targetLevel, RecommendationGuidance guidance,
            CandidateSafetyEvidence safetyEvidence,
            GoalDependencyProvenance goalProvenance,
            RecommendationStrategicValue strategicValue)
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
        this.goalProvenance = goalProvenance;
        this.strategicValue = strategicValue == null
                ? RecommendationStrategicValue.neutral() : strategicValue;
    }


    /** Active execution boundary; the distant strategic objective stays targetLevel. */
    public int getCurrentExecutionTargetLevel()
    {
        int stage = trainingPlan == null
                ? 0 : trainingPlan.getCurrentStageTargetLevel();
        return stage > 0 ? stage : targetLevel;
    }

    public Recommendation withGoalProvenance(GoalDependencyProvenance provenance)
    {
        return new Recommendation(id, title, reason, score, trainingPlan,
                confidence, currentLevel, targetLevel, guidance, safetyEvidence,
                provenance, strategicValue);
    }

    public Recommendation withStrategicValue(
            RecommendationStrategicValue value)
    {
        return new Recommendation(id, title, reason, score, trainingPlan,
                confidence, currentLevel, targetLevel, guidance, safetyEvidence,
                goalProvenance, value);
    }

    public Recommendation withGuidance(RecommendationGuidance value)
    {
        return new Recommendation(id, title, reason, score, trainingPlan,
                confidence, currentLevel, targetLevel, value, safetyEvidence,
                goalProvenance, strategicValue);
    }

    public Recommendation withSafetyEvidence(CandidateSafetyEvidence value)
    {
        return new Recommendation(id, title, reason, score, trainingPlan,
                confidence, currentLevel, targetLevel, guidance, value,
                goalProvenance, strategicValue);
    }
}
