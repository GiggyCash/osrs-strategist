package com.udderlywet.osrsstrategist;

import java.util.Objects;

import lombok.Getter;

/** One ordered, evidence-backed transition in the active goal plan. */
@Getter
public final class StrategicPlanStep
{
    private final String id;
    private final GoalNodeKind kind;
    private final String objective;
    private final String reason;
    private final PlanCompletionCondition completion;
    private final String recommendationId;

    public StrategicPlanStep(
            String id,
            GoalNodeKind kind,
            String objective,
            String reason,
            PlanCompletionCondition completion,
            String recommendationId)
    {
        if (id == null || id.trim().isEmpty()
                || objective == null || objective.trim().isEmpty())
            throw new IllegalArgumentException(Text.get(1207));
        this.id = id;
        this.kind = kind == null ? GoalNodeKind.META : kind;
        this.objective = objective.trim();
        this.reason = reason == null ? "" : reason.trim();
        this.completion = completion == null
                ? PlanCompletionCondition.none() : completion;
        this.recommendationId = recommendationId;
    }

    public boolean isComplete(GameData data)
    {
        return completion.isComplete(data);
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof StrategicPlanStep)) return false;
        StrategicPlanStep that = (StrategicPlanStep) other;
        return id.equals(that.id) && kind == that.kind
                && objective.equals(that.objective)
                && reason.equals(that.reason)
                && completion.equals(that.completion)
                && Objects.equals(recommendationId, that.recommendationId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, kind, objective, reason, completion,
                recommendationId);
    }
}
