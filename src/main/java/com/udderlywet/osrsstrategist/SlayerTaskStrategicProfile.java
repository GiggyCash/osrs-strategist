package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lombok.Getter;

/** Strategic task properties; decisions score these rather than task identities. */
public final class SlayerTaskStrategicProfile
{
    @Getter
    private final String taskProfileId;
    @Getter
    private final int xpQuality;
    @Getter
    private final int resourceValue;
    @Getter
    private final int completionBurden;
    @Getter
    private final int setupBurden;
    @Getter
    private final AttentionLevel attention;
    @Getter
    private final RiskLevel inherentRisk;
    @Getter
    private final SlayerRequiredItemUse requiredItemUse;
    @Getter
    private final CombatStyle requiredCombatStyle;
    private final Map<String, Integer> assignmentWeights;
    @Getter
    private final String alternativeActivityId;
    @Getter
    private final String alternativeName;
    @Getter
    private final String alternativeLocation;
    @Getter
    private final boolean directEncounter;

    public SlayerTaskStrategicProfile(String taskProfileId, int xpQuality,
            int resourceValue, int completionBurden, int setupBurden,
            AttentionLevel attention, RiskLevel inherentRisk,
            SlayerRequiredItemUse requiredItemUse,
            Map<String, Integer> assignmentWeights,
            String alternativeActivityId, String alternativeName,
            String alternativeLocation)
    {
        this(taskProfileId, xpQuality, resourceValue, completionBurden,
                setupBurden, attention, inherentRisk, requiredItemUse, null,
                assignmentWeights, alternativeActivityId, alternativeName,
                alternativeLocation, false);
    }

    public SlayerTaskStrategicProfile(String taskProfileId, int xpQuality,
            int resourceValue, int completionBurden, int setupBurden,
            AttentionLevel attention, RiskLevel inherentRisk,
            SlayerRequiredItemUse requiredItemUse,
            CombatStyle requiredCombatStyle,
            Map<String, Integer> assignmentWeights,
            String alternativeActivityId, String alternativeName,
            String alternativeLocation)
    {
        this(taskProfileId, xpQuality, resourceValue, completionBurden,
                setupBurden, attention, inherentRisk, requiredItemUse,
                requiredCombatStyle, assignmentWeights, alternativeActivityId,
                alternativeName, alternativeLocation, false);
    }

    public SlayerTaskStrategicProfile(String taskProfileId, int xpQuality,
            int resourceValue, int completionBurden, int setupBurden,
            AttentionLevel attention, RiskLevel inherentRisk,
            SlayerRequiredItemUse requiredItemUse,
            CombatStyle requiredCombatStyle,
            Map<String, Integer> assignmentWeights,
            String alternativeActivityId, String alternativeName,
            String alternativeLocation, boolean directEncounter)
    {
        this.taskProfileId = taskProfileId;
        this.xpQuality = scale(xpQuality);
        this.resourceValue = scale(resourceValue);
        this.completionBurden = scale(completionBurden);
        this.setupBurden = scale(setupBurden);
        this.attention = attention == null ? AttentionLevel.MODERATE : attention;
        this.inherentRisk = inherentRisk == null ? RiskLevel.LOW : inherentRisk;
        this.requiredItemUse = requiredItemUse == null
                ? SlayerRequiredItemUse.CARRIED_OR_EQUIPPED : requiredItemUse;
        this.requiredCombatStyle = requiredCombatStyle;
        this.assignmentWeights = Collections.unmodifiableMap(
                assignmentWeights == null ? Collections.emptyMap()
                        : new HashMap<>(assignmentWeights));
        this.alternativeActivityId = alternativeActivityId;
        this.alternativeName = alternativeName;
        this.alternativeLocation = alternativeLocation;
        this.directEncounter = directEncounter;
    }


    public Integer weightFor(String masterId)
    {
        return assignmentWeights.get(normalize(masterId));
    }

    private static int scale(int value)
    {
        return Math.max(1, Math.min(5, value));
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
