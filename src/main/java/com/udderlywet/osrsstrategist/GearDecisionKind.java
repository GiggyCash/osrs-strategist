package com.udderlywet.osrsstrategist;

/** Distinct questions in a contextual gear decision; none means universal BIS. */
public enum GearDecisionKind
{
    BEST_OWNED,
    BEST_USABLE,
    BEST_AVAILABLE_NOW,
    BEST_VALUE_UPGRADE,
    BEST_PRACTICAL_UPGRADE,
    LONG_TERM_TARGET,
    TARGET_SPECIFIC_BEST
}
