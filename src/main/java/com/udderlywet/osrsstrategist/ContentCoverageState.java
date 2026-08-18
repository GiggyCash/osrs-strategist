package com.udderlywet.osrsstrategist;

/** Explicit census disposition; no discovered content may silently disappear. */
public enum ContentCoverageState
{
    STRUCTURED,
    PARTIAL_PREPARATION,
    CONSERVATIVE_FAIL_CLOSED,
    NOT_PROGRESSION_RELEVANT
}
