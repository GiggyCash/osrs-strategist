package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One typed dependency inside a long-term goal graph. */
public final class GoalDependency
{
    @Getter
    private final String id;
    @Getter
    private final String label;
    @Getter
    private final GoalNodeKind kind;
    @Getter
    private final boolean required;

    public GoalDependency(
            String id,
            String label,
            GoalNodeKind kind,
            boolean required)
    {
        this.id = id;
        this.label = label;
        this.kind = kind == null ? GoalNodeKind.META : kind;
        this.required = required;
    }

}
