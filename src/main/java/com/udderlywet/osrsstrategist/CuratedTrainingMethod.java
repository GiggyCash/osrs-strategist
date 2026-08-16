package com.udderlywet.osrsstrategist;

/** A concrete method paired with the strategy metadata needed to rank it safely. */
public final class CuratedTrainingMethod
{
    private final TrainingMethod method;
    private final TrainingMethodMetadata metadata;

    public CuratedTrainingMethod(TrainingMethod method, TrainingMethodMetadata metadata)
    {
        this.method = method;
        this.metadata = metadata;
    }

    public TrainingMethod getMethod() { return method; }
    public TrainingMethodMetadata getMetadata() { return metadata; }
}
