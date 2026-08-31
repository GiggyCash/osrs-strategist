package compass;

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
    final String id;
    private final String title;
    private final String reason;
    private final double score;
    private final TrainingPlan trainingPlan;
    private final Confidence confidence;
    private final int currentLevel;
    private final int targetLevel;
    private final Guidance guidance;
    private final SafetyEvidence safetyEvidence;
    private final GoalProvenance goalProvenance;
    private final StrategicValue strategicValue;

    TrainingPlan plan() { return trainingPlan; }

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
                Confidence.CHECK_NEEDED,
                0,
                0,
                null,
                SafetyEvidence.unknown()
        );
    }

    /** Non-skill candidate form used by activity providers. */
    public Recommendation(String id, String title, String reason, double score,
            Confidence confidence)
    {
        this(id, title, reason, score, confidence, null,
                SafetyEvidence.unknown(),
                StrategicValue.neutral());
    }

    public Recommendation(String id, String title, String reason, double score,
            Confidence confidence, Guidance guidance)
    {
        this(id, title, reason, score, confidence, guidance,
                SafetyEvidence.unknown(),
                StrategicValue.neutral());
    }

    public Recommendation(String id, String title, String reason, double score,
            Confidence confidence, Guidance guidance,
            SafetyEvidence safetyEvidence)
    {
        this(id, title, reason, score, confidence, guidance, safetyEvidence,
                StrategicValue.neutral());
    }

    public Recommendation(String id, String title, String reason, double score,
            Confidence confidence, Guidance guidance,
            SafetyEvidence safetyEvidence,
            StrategicValue strategicValue)
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
            Confidence confidence)
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
                SafetyEvidence.unknown()
        );
    }

    public Recommendation(
            String id,
            String title,
            String reason,
            double score,
            TrainingPlan trainingPlan,
            Confidence confidence,
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
                SafetyEvidence.unknown()
        );
    }

    public Recommendation(
            String id,
            String title,
            String reason,
            double score,
            TrainingPlan trainingPlan,
            Confidence confidence,
            int currentLevel,
            int targetLevel,
            Guidance guidance)
    {
        this(id, title, reason, score, trainingPlan, confidence, currentLevel,
                targetLevel, guidance, SafetyEvidence.unknown());
    }

    public Recommendation(
            String id, String title, String reason, double score,
            TrainingPlan trainingPlan, Confidence confidence,
            int currentLevel, int targetLevel, Guidance guidance,
            SafetyEvidence safetyEvidence)
    {
        this(id, title, reason, score, trainingPlan, confidence, currentLevel,
                targetLevel, guidance, safetyEvidence, null,
                StrategicValue.neutral());
    }

    private Recommendation(
            String id, String title, String reason, double score,
            TrainingPlan trainingPlan, Confidence confidence,
            int currentLevel, int targetLevel, Guidance guidance,
            SafetyEvidence safetyEvidence,
            GoalProvenance goalProvenance,
            StrategicValue strategicValue)
    {
        this.id = id;
        this.title = title;
        this.reason = reason;
        this.score = score;
        this.trainingPlan = trainingPlan;
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED
                : confidence;
        this.currentLevel = Math.max(0, currentLevel);
        this.targetLevel = Math.max(0, targetLevel);
        this.guidance = guidance;
        this.safetyEvidence = safetyEvidence == null
                ? SafetyEvidence.unknown() : safetyEvidence;
        this.goalProvenance = goalProvenance;
        this.strategicValue = strategicValue == null
                ? StrategicValue.neutral() : strategicValue;
    }


    /** Active execution boundary; the distant strategic objective stays targetLevel. */
    public int getCurrentExecutionTargetLevel()
    {
        int stage = trainingPlan == null
                ? 0 : trainingPlan.getCurrentStageTargetLevel();
        return stage > 0 ? stage : targetLevel;
    }

    public Recommendation withGoalProvenance(GoalProvenance provenance)
    {
        return new Recommendation(id, title, reason, score, trainingPlan,
                confidence, currentLevel, targetLevel, guidance, safetyEvidence,
                provenance, strategicValue);
    }

    public Recommendation withStrategicValue(
            StrategicValue value)
    {
        return new Recommendation(id, title, reason, score, trainingPlan,
                confidence, currentLevel, targetLevel, guidance, safetyEvidence,
                goalProvenance, value);
    }

    public Recommendation withGuidance(Guidance value)
    {
        return new Recommendation(id, title, reason, score, trainingPlan,
                confidence, currentLevel, targetLevel, value, safetyEvidence,
                goalProvenance, strategicValue);
    }

    public Recommendation withSafetyEvidence(SafetyEvidence value)
    {
        return new Recommendation(id, title, reason, score, trainingPlan,
                confidence, currentLevel, targetLevel, guidance, value,
                goalProvenance, strategicValue);
    }
}
