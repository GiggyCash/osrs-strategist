package com.udderlywet.osrsstrategist;

import lombok.Getter;

public final class ClueSnapshot
{
    @Getter
    private final boolean cluePresent;
    @Getter
    private final String clueType;
    @Getter
    private final long firstSeenAtMillis;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
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





    public boolean hasObservedCurrentStep() { return currentStep != null; }
}
