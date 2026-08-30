package com.udderlywet.osrsstrategist;

public final class MoneyMakingMethod
{
    private final String id;
    private final String name;
    private final long estimatedGpPerHour;
    private final RecommendationConfidence confidence;

    public MoneyMakingMethod(
            String id,
            String name,
            long estimatedGpPerHour,
            RecommendationConfidence confidence)
    {
        this.id = id;
        this.name = name;
        this.estimatedGpPerHour = estimatedGpPerHour;
        this.confidence = confidence;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public long getEstimatedGpPerHour()
    {
        return estimatedGpPerHour;
    }

    public RecommendationConfidence getConfidence()
    {
        return confidence;
    }
}
