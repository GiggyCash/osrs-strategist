package com.udderlywet.osrsstrategist;

/** Result of the ordered UIM inventory-resolution policy. */
public final class UimInventoryResolution
{
    private final UimInventoryResolutionKind kind;
    private final RecommendationConfidence confidence;
    private final UimStorageDecision storageDecision;
    private final RecommendationRiskDisclosure riskDisclosure;
    private final String reason;

    public UimInventoryResolution(UimInventoryResolutionKind kind,
            RecommendationConfidence confidence,
            UimStorageDecision storageDecision,
            RecommendationRiskDisclosure riskDisclosure, String reason)
    {
        this.kind = kind;
        this.confidence = confidence;
        this.storageDecision = storageDecision;
        this.riskDisclosure = riskDisclosure;
        this.reason = reason;
    }

    public UimInventoryResolutionKind getKind() { return kind; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public UimStorageDecision getStorageDecision() { return storageDecision; }
    public RecommendationRiskDisclosure getRiskDisclosure()
    {
        return riskDisclosure;
    }
    public String getReason() { return reason; }
}
