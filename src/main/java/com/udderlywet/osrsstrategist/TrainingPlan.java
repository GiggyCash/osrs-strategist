package com.udderlywet.osrsstrategist;

/**
 * Selected method plus the confidence after evaluating the current account.
 *
 * <p>The static TrainingMethod definition describes the method in general;
 * this object describes whether that method is actually verified for this
 * character's current state.</p>
 */
public final class TrainingPlan
{
    private final TrainingMethod method;
    private final String whyThisMethod;
    private final RecommendationConfidence confidence;

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod)
    {
        this(
                method,
                whyThisMethod,
                method == null
                        ? RecommendationConfidence.CHECK_NEEDED
                        : method.getConfidence()
        );
    }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            RecommendationConfidence confidence)
    {
        this.method = method;
        this.whyThisMethod = whyThisMethod;
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED
                : confidence;
    }

    public TrainingMethod getMethod()
    {
        return method;
    }

    public String getWhyThisMethod()
    {
        return whyThisMethod;
    }

    public RecommendationConfidence getConfidence()
    {
        return confidence;
    }
}
