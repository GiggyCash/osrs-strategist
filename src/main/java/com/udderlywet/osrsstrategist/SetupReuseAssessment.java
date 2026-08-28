package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Evidence-backed value of preserving the player's currently observed setup. */
public final class SetupReuseAssessment
{
    private final RecommendationConfidence confidence;
    private final int matchedProperties;
    private final int requiredProperties;
    private final int minutesAvoided;
    private final double normalizedValue;
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

    public RecommendationConfidence getConfidence() { return confidence; }
    public int getMatchedProperties() { return matchedProperties; }
    public int getRequiredProperties() { return requiredProperties; }
    public int getMinutesAvoided() { return minutesAvoided; }
    public double getNormalizedValue() { return normalizedValue; }
    public List<String> getEvidence() { return evidence; }

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
