package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Strategic task properties; decisions score these rather than task identities. */
public final class SlayerTaskStrategicProfile
{
    private final String taskProfileId;
    private final int xpQuality;
    private final int resourceValue;
    private final int completionBurden;
    private final int setupBurden;
    private final AttentionLevel attention;
    private final RiskLevel inherentRisk;
    private final SlayerRequiredItemUse requiredItemUse;
    private final CombatStyle requiredCombatStyle;
    private final Map<String, Integer> assignmentWeights;
    private final String alternativeActivityId;
    private final String alternativeName;
    private final String alternativeLocation;

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
                alternativeLocation);
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
    }

    public String getTaskProfileId() { return taskProfileId; }
    public int getXpQuality() { return xpQuality; }
    public int getResourceValue() { return resourceValue; }
    public int getCompletionBurden() { return completionBurden; }
    public int getSetupBurden() { return setupBurden; }
    public AttentionLevel getAttention() { return attention; }
    public RiskLevel getInherentRisk() { return inherentRisk; }
    public SlayerRequiredItemUse getRequiredItemUse() { return requiredItemUse; }
    public CombatStyle getRequiredCombatStyle() { return requiredCombatStyle; }
    public String getAlternativeActivityId() { return alternativeActivityId; }
    public String getAlternativeName() { return alternativeName; }
    public String getAlternativeLocation() { return alternativeLocation; }

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
