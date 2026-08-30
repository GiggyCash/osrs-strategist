package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Result of checking one proposed UIM storage route. */
public final class UimStorageDecision
{
    @Getter
    private final StorageCapability capability;
    @Getter
    private final boolean allowed;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final RiskLevel riskLevel;
    @Getter
    private final String explanation;

    public UimStorageDecision(
            StorageCapability capability,
            boolean allowed,
            RecommendationConfidence confidence,
            RiskLevel riskLevel,
            String explanation)
    {
        this.capability = capability;
        this.allowed = allowed;
        this.confidence = confidence;
        this.riskLevel = riskLevel == null ? RiskLevel.NONE : riskLevel;
        this.explanation = explanation;
    }

}
