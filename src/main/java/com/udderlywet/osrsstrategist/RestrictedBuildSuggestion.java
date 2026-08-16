package com.udderlywet.osrsstrategist;

public final class RestrictedBuildSuggestion
{
    private final RestrictedBuildType type;
    private final RecommendationConfidence confidence;
    private final String evidence;

    public RestrictedBuildSuggestion(RestrictedBuildType type,
            RecommendationConfidence confidence, String evidence)
    {
        this.type = type;
        this.confidence = confidence;
        this.evidence = evidence;
    }

    public RestrictedBuildType getType() { return type; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public String getEvidence() { return evidence; }
}
