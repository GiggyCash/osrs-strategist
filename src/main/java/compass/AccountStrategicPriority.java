package compass;

import lombok.Getter;

/** One explainable account-mode/state contribution. */
@Getter
public final class AccountStrategicPriority
{
    private final AccountStrategicDimension dimension;
    private final StrategicPriority priority;
    private final CapabilityState capabilityState;
    private final Confidence confidence;
    private final String reason;

    public AccountStrategicPriority(
            AccountStrategicDimension dimension,
            StrategicPriority priority,
            CapabilityState capabilityState,
            Confidence confidence,
            String reason)
    {
        if (dimension == null) throw new IllegalArgumentException("dimension");
        this.dimension = dimension;
        this.priority = priority == null ? StrategicPriority.NONE : priority;
        this.capabilityState = capabilityState == null
                ? CapabilityState.UNKNOWN : capabilityState;
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED : confidence;
        this.reason = reason == null ? "" : reason;
    }

}
