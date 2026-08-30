package com.udderlywet.osrsstrategist;

public final class TrainingPlan
{
    private final TrainingMethod method;
    private final String whyThisMethod;

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod)
    {
        this.method = method;
        this.whyThisMethod = whyThisMethod;
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
        return method == null
                ? RecommendationConfidence.CHECK_NEEDED
                : method.getConfidence();
    }
}
