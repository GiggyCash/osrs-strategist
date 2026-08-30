package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** A concrete method paired with the strategy metadata needed to rank it safely. */
@RequiredArgsConstructor
public final class CuratedTrainingMethod
{
    @Getter
    private final TrainingMethod method;
    @Getter
    private final TrainingMethodMetadata metadata;


}
