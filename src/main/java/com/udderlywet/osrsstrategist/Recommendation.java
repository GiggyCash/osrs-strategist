package com.udderlywet.osrsstrategist;

public final class Recommendation
{
    private final String id;
    private final String title;
    private final String reason;
    private final double score;
    private final TrainingPlan trainingPlan;
    private final RecommendationConfidence confidence;

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
                RecommendationConfidence.CHECK_NEEDED
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
        this.id = id;
        this.title = title;
        this.reason = reason;
        this.score = score;
        this.trainingPlan = trainingPlan;
        this.confidence = confidence;
    }

    public String getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public String getReason()
    {
        return reason;
    }

    public double getScore()
    {
        return score;
    }

    public TrainingPlan getTrainingPlan()
    {
        return trainingPlan;
    }

    public RecommendationConfidence getConfidence()
    {
        return confidence;
    }
}
