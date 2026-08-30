package com.udderlywet.osrsstrategist;

/** One typed dependency inside a long-term goal graph. */
public final class GoalDependency
{
    private final String id;
    private final String label;
    private final GoalNodeKind kind;
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

    public String getId() { return id; }
    public String getLabel() { return label; }
    public GoalNodeKind getKind() { return kind; }
    public boolean isRequired() { return required; }
}
