package compass;

import java.util.*;

/** Evidence-backed steps, separate from access and physical capacity. Planning only. */
final class MethodPreparation
{
    enum State { READY, VERIFIED, UNRESOLVED, BLOCKED }
    enum Kind { CARRIED, EQUIPPED, RETRIEVE, BUY, ACQUIRE, UNSUPPORTED }

    static final class Step
    {
        final Kind kind;
        final MethodInput input;
        final RequirementState evidence;
        final String reason;

        Step(Kind kind, MethodInput input, RequirementState evidence, String reason)
        {
            this.kind = kind;
            this.input = input;
            this.evidence = evidence;
            this.reason = reason;
        }
    }

    final State state;
    final List<Step> steps;
    final long purchaseCost;

    MethodPreparation(State state, List<Step> steps, long purchaseCost)
    {
        this.state = state;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        this.purchaseCost = purchaseCost;
    }

    boolean feasible() { return state == State.READY || state == State.VERIFIED; }

    String explanation()
    {
        List<String> reasons = new ArrayList<>();
        for (Step step : steps) reasons.add(step.reason);
        return String.join(" ", reasons);
    }
}
