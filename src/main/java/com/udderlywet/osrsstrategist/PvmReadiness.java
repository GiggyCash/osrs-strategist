package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

public final class PvmReadiness
{
    @Getter
    private final String activityId;
    @Getter
    private final boolean realisticallyReady;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final List<String> missingRequirements;

    public PvmReadiness(
            String activityId,
            boolean realisticallyReady,
            RecommendationConfidence confidence,
            List<String> missingRequirements)
    {
        this.activityId = activityId;
        this.realisticallyReady = realisticallyReady;
        this.confidence = confidence;
        this.missingRequirements = Collections.unmodifiableList(
                new ArrayList<>(missingRequirements)
        );
    }



    /** Conservative beta contract: observed carried setup is ready to attempt. */
    public boolean isReadyForRecommendation()
    {
        return realisticallyReady && confidence == RecommendationConfidence.VERIFIED;
    }


}
