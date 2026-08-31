package compass;

import java.util.*;

import lombok.Getter;

/** Result of evaluating a composable item requirement against observed state. */
public final class ItemRequirementResult
{
    @Getter
    private final RequirementState state;
    @Getter
    private final String action;
    private final List<MethodInput> missingInputs;

    public ItemRequirementResult(RequirementState state, String action)
    {
        this(state, action, Collections.emptyList());
    }

    public ItemRequirementResult(RequirementState state, String action,
            List<MethodInput> missingInputs)
    {
        this.state = state;
        this.action = action == null ? "" : action;
        this.missingInputs = Collections.unmodifiableList(missingInputs == null
                ? new ArrayList<>() : new ArrayList<>(missingInputs));
    }

    /** Exact, evidence-backed shortfalls only. Unknown storage never appears here. */
    public List<MethodInput> getMissingInputs() { return missingInputs; }
    public boolean isSatisfied() { return state == RequirementState.VERIFIED; }
}
