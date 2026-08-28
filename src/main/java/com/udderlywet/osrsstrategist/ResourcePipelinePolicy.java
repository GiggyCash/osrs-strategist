package com.udderlywet.osrsstrategist;

/** Verified item-family semantics required before resource value is scored. */
public final class ResourcePipelinePolicy
{
    private final ResourceUseKind useKind;
    private final ResourceScarcity scarcity;
    private final boolean tradeable;

    public ResourcePipelinePolicy(ResourceUseKind useKind,
            ResourceScarcity scarcity, boolean tradeable)
    {
        this.useKind = useKind;
        this.scarcity = scarcity;
        this.tradeable = tradeable;
    }

    public ResourceUseKind getUseKind() { return useKind; }
    public ResourceScarcity getScarcity() { return scarcity; }
    public boolean isTradeable() { return tradeable; }
}
