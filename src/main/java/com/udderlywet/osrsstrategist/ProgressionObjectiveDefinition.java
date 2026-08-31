package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A longer-running reward objective associated with a training method. These
 * objectives outrank tiny variety nudges until completion is actually known.
 */
@Getter
@RequiredArgsConstructor
public final class ProgressionObjectiveDefinition
{
    private final String id;
    private final String title;
    private final String methodId;
    private final ProgressionObjectiveType type;


}
