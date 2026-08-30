package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One ordered, actionable edge in a STASH preparation chain. */
public final class StashDependencyStep
{
    @Getter
    private final GoalNodeKind kind;
    @Getter
    private final String action;
    @Getter
    private final RecommendationConfidence confidence;

    public StashDependencyStep(GoalNodeKind kind, String action,
            RecommendationConfidence confidence)
    {
        this.kind = kind;
        this.action = action == null ? "" : action;
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED : confidence;
    }

}
