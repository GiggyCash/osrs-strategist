package compass;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

/** Strategy/account-mode metadata layered on top of a concrete training method. */
@RequiredArgsConstructor
@Getter
public final class TrainingMethodMetadata
{
    private final TrainingIntensity intensity;
    private final MethodCostTier costTier;
    private final RiskLevel riskLevel;
    private final boolean freeToPlayAllowed;
    private final boolean selfSourceFriendly;
    private final boolean uimFriendly;
    private final boolean hardcoreSafe;
    private final List<String> tags;



    public static TrainingMethodMetadata legacy(TrainingMethod method)
    {
        return new TrainingMethodMetadata(
                TrainingIntensity.BALANCED,
                MethodCostTier.LOW,
                method != null && method.isWilderness() ? RiskLevel.HIGH : RiskLevel.NONE,
                method == null || !method.isMembersOnly(),
                true,
                true,
                method == null || !method.isWilderness(),
                Collections.singletonList("legacy")
        );
    }
}
