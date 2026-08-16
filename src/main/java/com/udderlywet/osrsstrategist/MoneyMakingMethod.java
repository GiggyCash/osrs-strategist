package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MoneyMakingMethod
{
    private final String id;
    private final String name;
    private final long estimatedGpPerHour;
    private final RecommendationConfidence confidence;
    private final boolean membersOnly;
    private final boolean wilderness;
    private final boolean tradeDependent;
    private final List<String> requirements;
    private final String valueSummary;

    public MoneyMakingMethod(
            String id,
            String name,
            long estimatedGpPerHour,
            RecommendationConfidence confidence)
    {
        this(id, name, estimatedGpPerHour, confidence,
                false, false, false, Collections.emptyList(), null);
    }

    public MoneyMakingMethod(String id, String name, long estimatedGpPerHour,
            RecommendationConfidence confidence, boolean membersOnly,
            boolean wilderness, boolean tradeDependent,
            List<String> requirements, String valueSummary)
    {
        this.id = id;
        this.name = name;
        this.estimatedGpPerHour = estimatedGpPerHour;
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED : confidence;
        this.membersOnly = membersOnly;
        this.wilderness = wilderness;
        this.tradeDependent = tradeDependent;
        this.requirements = Collections.unmodifiableList(
                requirements == null ? new ArrayList<>() : new ArrayList<>(requirements));
        this.valueSummary = valueSummary;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getEstimatedGpPerHour() { return estimatedGpPerHour; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public boolean isMembersOnly() { return membersOnly; }
    public boolean isWilderness() { return wilderness; }
    public boolean isTradeDependent() { return tradeDependent; }
    public List<String> getRequirements() { return requirements; }
    public String getValueSummary() { return valueSummary; }
}
