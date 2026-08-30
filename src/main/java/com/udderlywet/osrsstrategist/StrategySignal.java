package com.udderlywet.osrsstrategist;

/**
 * A small, testable contribution from one subsystem to the strategy engine.
 *
 * <p>Signals are not necessarily shown directly to the player. They are the
 * internal explanation of why something matters. Keeping them structured lets
 * the UI stay concise while a Details view can expose the deeper reasoning.</p>
 */
public final class StrategySignal
{
    private final String id;
    private final StrategySignalCategory category;
    private final String summary;
    private final double scoreDelta;
    private final RecommendationConfidence confidence;

    public StrategySignal(
            String id,
            StrategySignalCategory category,
            String summary,
            double scoreDelta,
            RecommendationConfidence confidence)
    {
        this.id = id;
        this.category = category;
        this.summary = summary;
        this.scoreDelta = scoreDelta;
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED
                : confidence;
    }

    public String getId() { return id; }
    public StrategySignalCategory getCategory() { return category; }
    public String getSummary() { return summary; }
    public double getScoreDelta() { return scoreDelta; }
    public RecommendationConfidence getConfidence() { return confidence; }
}
