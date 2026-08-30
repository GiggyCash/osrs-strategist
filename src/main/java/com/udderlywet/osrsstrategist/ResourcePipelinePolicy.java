package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Verified item-family semantics required before resource value is scored. */
@RequiredArgsConstructor
public final class ResourcePipelinePolicy
{
    @Getter
    private final ResourceUseKind useKind;
    @Getter
    private final ResourceScarcity scarcity;
    @Getter
    private final boolean tradeable;


}
