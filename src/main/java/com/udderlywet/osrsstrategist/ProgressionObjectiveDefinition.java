package com.udderlywet.osrsstrategist;

/**
 * A longer-running reward objective associated with a training method. These
 * objectives outrank tiny variety nudges until completion is actually known.
 */
public final class ProgressionObjectiveDefinition
{
    private final String id;
    private final String title;
    private final String methodId;
    private final ProgressionObjectiveType type;

    public ProgressionObjectiveDefinition(
            String id,
            String title,
            String methodId,
            ProgressionObjectiveType type)
    {
        this.id = id;
        this.title = title;
        this.methodId = methodId;
        this.type = type;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMethodId() { return methodId; }
    public ProgressionObjectiveType getType() { return type; }
}
