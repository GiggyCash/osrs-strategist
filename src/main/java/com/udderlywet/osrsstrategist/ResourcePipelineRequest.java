package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * Typed input for consumable value. Tradeability and scarcity must come from
 * verified item/plan data; this service never guesses them from an item name.
 */
@Getter
public final class ResourcePipelineRequest
{
    private final ResourceNeed need;
    private final ResourceUseKind useKind;
    private final ResourceScarcity scarcity;
    private final boolean tradeable;

    public ResourcePipelineRequest(ResourceNeed need, ResourceUseKind useKind,
            ResourceScarcity scarcity, boolean tradeable)
    {
        this.need = need;
        this.useKind = useKind == null
                ? ResourceUseKind.ONE_OFF_CONSUMABLE : useKind;
        this.scarcity = scarcity == null
                ? ResourceScarcity.UNKNOWN : scarcity;
        this.tradeable = tradeable;
    }

}
