package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One ordered, non-destructive step in a resource acquisition chain. */
public final class ResourceAcquisitionStep
{
    @Getter
    private final AcquisitionSource source;
    @Getter
    private final String action;
    @Getter
    private final RecommendationConfidence confidence;

    public ResourceAcquisitionStep(AcquisitionSource source, String action,
            RecommendationConfidence confidence)
    {
        this.source = source;
        this.action = action;
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED : confidence;
    }

}
