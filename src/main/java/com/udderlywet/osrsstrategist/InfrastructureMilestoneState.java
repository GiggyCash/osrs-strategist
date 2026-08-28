package com.udderlywet.osrsstrategist;

/** Readiness/completion state for an infrastructure unlock. */
public enum InfrastructureMilestoneState
{
    COMPLETE,
    ACTIONABLE,
    CHECK_NEEDED,
    REQUIREMENTS_MISSING,
    NOT_APPLICABLE
}
