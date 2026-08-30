package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * One ranked action the player could take next.
 *
 * <p>Current/target levels are structured fields instead of being buried in a
 * paragraph. That lets the sidebar stay compact while Details can still expose
 * the full reasoning.</p>
 */
public final class Recommendation
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
    private final TrainingPlan trainingPlan;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final int currentLevel;
    @Getter
    private final int targetLevel;
    @Getter
    private final RecommendationGuidance guidance;
    @Getter
    private final CandidateSafetyEvidence safetyEvidence;
    @Getter
    private final GoalDependencyProvenance goalProvenance;
    @Getter
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
