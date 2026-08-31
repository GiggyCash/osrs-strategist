package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Evidence-backed value of preserving the player's currently observed setup. */
public final class SetupReuseAssessment
{
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final int matchedProperties;
    @Getter
    private final int requiredProperties;
    @Getter
    private final int minutesAvoided;
    @Getter
    private final double normalizedValue;
    @Getter
    private final List<String> evidence;

    SetupReuseAssessment(RecommendationConfidence confidence,
            int matchedProperties, int requiredProperties, int minutesAvoided,
            double normalizedValue, List<String> evidence)
    {
        this.confidence = confidence;
        this.matchedProperties = matchedProperties;
        this.requiredProperties = requiredProperties;
        this.minutesAvoided = Math.max(0, minutesAvoided);
        this.normalizedValue = Math.max(0.0, Math.min(1.0, normalizedValue));
        this.evidence = Collections.unmodifiableList(new ArrayList<>(evidence));
    }


    public RecommendationStrategicValue strategicValue(String evidenceId)
    {
        if (normalizedValue <= 0.0)
            return RecommendationStrategicValue.neutral();
        RecommendationStrategicValue.Builder builder =
                RecommendationStrategicValue.builder()
                        .setupReuse(normalizedValue);
        if (evidenceId != null && !evidenceId.trim().isEmpty())
            builder.evidence(evidenceId);
        for (String item : evidence) builder.evidence(item);
        return builder.build();
    }
}
