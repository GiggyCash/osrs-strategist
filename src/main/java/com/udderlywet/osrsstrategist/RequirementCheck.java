package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * One human-readable readiness check shown by the recommendation UI.
 *
 * <p>The evidence text explains why Compass reached the state. This makes
 * "Check Needed" actionable instead of a vague warning.</p>
 */
public final class RequirementCheck
{
    @Getter
    private final String id;
    @Getter
    private final String label;
    @Getter
    private final RequirementState state;
    @Getter
    private final String evidence;

    public RequirementCheck(
            String id,
            String label,
            RequirementState state,
            String evidence)
    {
        this.id = id;
        this.label = label;
        this.state = state == null
                ? RequirementState.CHECK_NEEDED
                : state;
        this.evidence = evidence;
    }

}
