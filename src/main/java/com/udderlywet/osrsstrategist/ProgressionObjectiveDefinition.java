package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A longer-running reward objective associated with a training method. These
 * objectives outrank tiny variety nudges until completion is actually known.
 */
@RequiredArgsConstructor
public final class ProgressionObjectiveDefinition
{
    @Getter
    private final String id;
    @Getter
    private final String title;
    @Getter
    private final String methodId;
    @Getter
    private final ProgressionObjectiveType type;


}
