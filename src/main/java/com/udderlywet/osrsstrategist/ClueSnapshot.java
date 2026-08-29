package com.udderlywet.osrsstrategist;

public final class ClueSnapshot
{
    private final boolean cluePresent;
    private final String clueType;
    private final long firstSeenAtMillis;
    private final RecommendationConfidence confidence;
    private final ClueStepSnapshot currentStep;

    public ClueSnapshot(
            boolean cluePresent,
            String clueType,
            long firstSeenAtMillis,
            RecommendationConfidence confidence)
    {
        this(cluePresent, clueType, firstSeenAtMillis, confidence, null);
    }

    public ClueSnapshot(
            boolean cluePresent,
            String clueType,
            long firstSeenAtMillis,
            RecommendationConfidence confidence,
            ClueStepSnapshot currentStep)
    {
        this.cluePresent = cluePresent;
        this.clueType = clueType;
        this.firstSeenAtMillis = firstSeenAtMillis;
        this.confidence = confidence;
        this.currentStep = currentStep;
    }

    public boolean isCluePresent()
    {
        return cluePresent;
    }

    public String getClueType()
    {
        return clueType;
    }

    public long getFirstSeenAtMillis()
    {
        return firstSeenAtMillis;
    }

    public RecommendationConfidence getConfidence()
    {
        return confidence;
    }

    public ClueStepSnapshot getCurrentStep() { return currentStep; }
    public boolean hasObservedCurrentStep() { return currentStep != null; }
}
