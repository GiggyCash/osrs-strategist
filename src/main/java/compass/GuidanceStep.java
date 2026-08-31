package compass;

import lombok.Getter;

@Getter
public final class GuidanceStep
{
    final String id;
    private final String label;
    private final String detail;
    private final GuidanceStepState state;

    public GuidanceStep(
            String id,
            String label,
            String detail,
            GuidanceStepState state)
    {
        this.id = id;
        this.label = label;
        this.detail = detail;
        this.state = state == null ? GuidanceStepState.CHECK_NEEDED : state;
    }

    public boolean isComplete() { return state == GuidanceStepState.COMPLETE; }
}
