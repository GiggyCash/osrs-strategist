package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Result of checking one proposed UIM storage route. */
@Getter
public final class UimStorageDecision
{
    private final StorageCapability capability;
    private final boolean allowed;
    private final Confidence confidence;
    private final RiskLevel riskLevel;
    private final String explanation;

    public UimStorageDecision(
            StorageCapability capability,
            boolean allowed,
            Confidence confidence,
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
