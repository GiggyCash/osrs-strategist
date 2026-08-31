package compass;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Result of the ordered UIM inventory-resolution policy. */
@Getter
@RequiredArgsConstructor
public final class UimInventoryResolution
{
    private final UimInventoryResolutionKind kind;
    private final Confidence confidence;
    private final UimStorageDecision storageDecision;
    private final RecommendationRiskDisclosure riskDisclosure;
    private final String reason;


}
