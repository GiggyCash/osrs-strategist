package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of evaluating a composable item requirement against observed state. */
public final class ItemRequirementResult
{
    private final RequirementState state;
    private final String action;
    private final List<ResolvedMethodInput> missingInputs;

    public ItemRequirementResult(RequirementState state, String action)
    {
        this(state, action, Collections.emptyList());
    }

    public ItemRequirementResult(RequirementState state, String action,
            List<ResolvedMethodInput> missingInputs)
    {
        this.state = state;
        this.action = action == null ? "" : action;
        this.missingInputs = Collections.unmodifiableList(missingInputs == null
                ? new ArrayList<>() : new ArrayList<>(missingInputs));
    }

    public RequirementState getState() { return state; }
    public String getAction() { return action; }
    /** Exact, evidence-backed shortfalls only. Unknown storage never appears here. */
    public List<ResolvedMethodInput> getMissingInputs() { return missingInputs; }
    public boolean isSatisfied() { return state == RequirementState.VERIFIED; }
}
