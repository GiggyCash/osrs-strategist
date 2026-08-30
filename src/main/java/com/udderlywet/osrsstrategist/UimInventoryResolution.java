package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Result of the ordered UIM inventory-resolution policy. */
@RequiredArgsConstructor
public final class UimInventoryResolution
{
    @Getter
    private final UimInventoryResolutionKind kind;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final UimStorageDecision storageDecision;
    @Getter
    private final RecommendationRiskDisclosure riskDisclosure;
    @Getter
    private final String reason;


}
