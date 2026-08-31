package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Verified item-family semantics required before resource value is scored. */
@Getter
@RequiredArgsConstructor
public final class ResourcePipelinePolicy
{
    private final ResourceUseKind useKind;
    private final ResourceScarcity scarcity;
    private final boolean tradeable;


}
