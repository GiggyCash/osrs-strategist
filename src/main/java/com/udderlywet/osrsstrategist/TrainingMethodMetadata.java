package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Strategy/account-mode metadata layered on top of a concrete training method. */
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

    public TrainingIntensity getIntensity() { return intensity; }
    public MethodCostTier getCostTier() { return costTier; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public boolean isFreeToPlayAllowed() { return freeToPlayAllowed; }
    public boolean isSelfSourceFriendly() { return selfSourceFriendly; }
    public boolean isUimFriendly() { return uimFriendly; }
    public boolean isHardcoreSafe() { return hardcoreSafe; }
    public List<String> getTags() { return tags; }

    public static TrainingMethodMetadata legacy(TrainingMethod method)
    {
        return new TrainingMethodMetadata(
                TrainingIntensity.BALANCED,
                MethodCostTier.LOW,
                method != null && method.isWilderness() ? RiskLevel.HIGH : RiskLevel.NONE,
                false,
                true,
                true,
                method == null || !method.isWilderness(),
                Collections.singletonList("legacy")
        );
    }
}
