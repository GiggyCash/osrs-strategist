package com.udderlywet.osrsstrategist;

/**
 * One human-readable readiness check shown by the recommendation UI.
 *
 * <p>The evidence text explains why Strategist reached the state. This makes
 * "Check Needed" actionable instead of a vague warning.</p>
 */
public final class RequirementCheck
{
    private final String id;
    private final String label;
    private final RequirementState state;
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

    public String getId() { return id; }
    public String getLabel() { return label; }
    public RequirementState getState() { return state; }
    public String getEvidence() { return evidence; }
}
