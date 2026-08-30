package com.udderlywet.osrsstrategist;

/** Result of checking one proposed UIM storage route. */
public final class UimStorageDecision
{
    private final StorageCapability capability;
    private final boolean allowed;
    private final RecommendationConfidence confidence;
    private final RiskLevel riskLevel;
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

    public StorageCapability getCapability() { return capability; }
    public boolean isAllowed() { return allowed; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public String getExplanation() { return explanation; }
}
