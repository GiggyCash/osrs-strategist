package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * A small, testable contribution from one subsystem to the strategy engine.
 *
 * <p>Signals are not necessarily shown directly to the player. They are the
 * internal explanation of why something matters. Keeping them structured lets
 * the UI stay concise while a Details view can expose the deeper reasoning.</p>
 */
public final class StrategySignal
{
    @Getter
    private final String id;
    @Getter
    private final StrategySignalCategory category;
    @Getter
    private final String summary;
    @Getter
    private final double scoreDelta;
    @Getter
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

}
