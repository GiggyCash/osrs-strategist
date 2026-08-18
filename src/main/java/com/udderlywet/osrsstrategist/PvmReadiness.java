package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PvmReadiness
{
    private final String activityId;
    private final boolean realisticallyReady;
    private final RecommendationConfidence confidence;
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

    public String getActivityId()
    {
        return activityId;
    }

    public boolean isRealisticallyReady()
    {
        return realisticallyReady;
    }

    /** Conservative beta contract: observed carried setup is ready to attempt. */
    public boolean isReadyForRecommendation()
    {
        return realisticallyReady && confidence == RecommendationConfidence.VERIFIED;
    }

    public RecommendationConfidence getConfidence()
    {
        return confidence;
    }

    public List<String> getMissingRequirements()
    {
        return missingRequirements;
    }
}
