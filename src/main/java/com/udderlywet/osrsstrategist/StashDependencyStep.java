package com.udderlywet.osrsstrategist;

/** One ordered, actionable edge in a STASH preparation chain. */
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

    public GoalNodeKind getKind() { return kind; }
    public String getAction() { return action; }
    public RecommendationConfidence getConfidence() { return confidence; }
}
