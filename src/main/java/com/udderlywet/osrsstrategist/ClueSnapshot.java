package com.udderlywet.osrsstrategist;

import lombok.Getter;

@Getter
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





    public boolean hasObservedCurrentStep() { return currentStep != null; }
}
