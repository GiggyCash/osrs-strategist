package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One ordered, actionable edge in a STASH preparation chain. */
@Getter
public final class StashDependencyStep
{
    private final GoalNodeKind kind;
    private final String action;
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
