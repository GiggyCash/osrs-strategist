package com.udderlywet.osrsstrategist;

/** Built/filled state is never inferred from skill level or prior clue access. */
public enum StashUnitState
{
    UNKNOWN,
    NOT_BUILT,
    BUILT_CONTENTS_UNKNOWN,
    BUILT_AND_FILLED
}
