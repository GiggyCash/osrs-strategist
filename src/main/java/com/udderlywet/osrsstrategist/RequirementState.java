package com.udderlywet.osrsstrategist;

/**
 * Result of checking one concrete recommendation requirement.
 *
 * <p>VERIFIED means Strategist has positive evidence, CHECK_NEEDED means the
 * requirement is plausible but not yet proven, and BLOCKED means the current
 * account is known not to satisfy it.</p>
 */
public enum RequirementState
{
    VERIFIED,
    CHECK_NEEDED,
    BLOCKED
}
