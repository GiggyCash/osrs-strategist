package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

@Getter
public final class PvmReadiness
{
    private final String activityId;
    private final boolean realisticallyReady;
    private final Confidence confidence;
    private final List<String> missingRequirements;

    public PvmReadiness(
            String activityId,
            boolean realisticallyReady,
            Confidence confidence,
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
        return realisticallyReady && confidence == Confidence.VERIFIED;
    }


}
