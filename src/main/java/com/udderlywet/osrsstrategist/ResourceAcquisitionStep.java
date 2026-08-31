package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One ordered, non-destructive step in a resource acquisition chain. */
@Getter
public final class ResourceAcquisitionStep
{
    private final AcquisitionSource source;
    private final String action;
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
