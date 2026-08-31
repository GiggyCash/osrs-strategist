package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Strategy/account-mode metadata layered on top of a concrete training method. */
public final class TrainingMethodMetadata
{
    @Getter
    private final TrainingIntensity intensity;
    @Getter
    private final MethodCostTier costTier;
    @Getter
    private final RiskLevel riskLevel;
    @Getter
    private final boolean freeToPlayAllowed;
    @Getter
    private final boolean selfSourceFriendly;
    @Getter
    private final boolean uimFriendly;
    @Getter
    private final boolean hardcoreSafe;
    @Getter
    private final List<String> tags;

    public TrainingMethodMetadata(
            TrainingIntensity intensity,
            MethodCostTier costTier,
            RiskLevel riskLevel,
            boolean freeToPlayAllowed,
            boolean selfSourceFriendly,
            boolean uimFriendly,
            boolean hardcoreSafe,
            List<String> tags)
    {
        this.intensity = intensity == null ? TrainingIntensity.BALANCED : intensity;
        this.costTier = costTier == null ? MethodCostTier.LOW : costTier;
        this.riskLevel = riskLevel == null ? RiskLevel.NONE : riskLevel;
        this.freeToPlayAllowed = freeToPlayAllowed;
        this.selfSourceFriendly = selfSourceFriendly;
        this.uimFriendly = uimFriendly;
        this.hardcoreSafe = hardcoreSafe;
        this.tags = Collections.unmodifiableList(tags == null
                ? new ArrayList<>() : new ArrayList<>(tags));
    }


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
