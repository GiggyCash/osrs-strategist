package compass;

import java.util.*;

import lombok.Getter;

/**
 * Sailing discovery/progression state.
 *
 * <p>Sailing is expected to evolve quickly. Keeping ports and activities as
 * data keys means the recommendation engine can gain new Sailing coverage by
 * updating structured game data instead of changing its core algorithm.</p>
 */
@Getter
public final class SailingSnapshot
{
    public static final String PORT_SARIM = "port:sarim";
    public static final String PORT_PANDEMONIUM = Text.get(1960);
    public static final String ACTIVITY_COURIER = Text.get(1961);
    public static final String ACTIVITY_ACTIVE_PORT_TASK = Text.get(1962);
    public static final String ACTIVITY_SEA_CHARTING = Text.get(1963);
    public static final String ACTIVITY_BOAT_OWNED = Text.get(1964);
    public static final String TRIAL_TEMPOR_COMPLETE = Text.get(1965);
    public static final String TRIAL_JUBBLY_COMPLETE = Text.get(1966);
    public static final String TRIAL_GWENITH_COMPLETE = Text.get(1967);

    private final Set<String> verifiedPorts;
    private final Set<String> verifiedActivities;
    private final Confidence confidence;

    public SailingSnapshot(
            Set<String> verifiedPorts,
            Set<String> verifiedActivities,
            Confidence confidence)
    {
        this.verifiedPorts = Collections.unmodifiableSet(
                verifiedPorts == null
                        ? new HashSet<>()
                        : new HashSet<>(verifiedPorts)
        );
        this.verifiedActivities = Collections.unmodifiableSet(
                verifiedActivities == null
                        ? new HashSet<>()
                        : new HashSet<>(verifiedActivities)
        );
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED
                : confidence;
    }

    public static SailingSnapshot unknown()
    {
        return new SailingSnapshot(
                Collections.emptySet(),
                Collections.emptySet(),
                Confidence.CHECK_NEEDED
        );
    }

    public boolean hasPort(String portId)
    {
        return portId != null && verifiedPorts.contains(portId);
    }

    public boolean hasActivity(String activityId)
    {
        return activityId != null && verifiedActivities.contains(activityId);
    }

}
