package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;

/**
 * Sailing discovery/progression state.
 *
 * <p>Sailing is expected to evolve quickly. Keeping ports and activities as
 * data keys means the recommendation engine can gain new Sailing coverage by
 * updating structured game data instead of changing its core algorithm.</p>
 */
public final class SailingSnapshot
{
    public static final String PORT_SARIM = "port:sarim";
    public static final String PORT_PANDEMONIUM = "port:pandemonium";
    public static final String ACTIVITY_COURIER = "activity:courier";
    public static final String ACTIVITY_ACTIVE_PORT_TASK = "activity:active-port-task";
    public static final String ACTIVITY_SEA_CHARTING = "activity:sea-charting";
    public static final String ACTIVITY_BOAT_OWNED = "activity:boat-owned";
    public static final String TRIAL_TEMPOR_COMPLETE = "trial:tempor-complete";
    public static final String TRIAL_JUBBLY_COMPLETE = "trial:jubbly-complete";
    public static final String TRIAL_GWENITH_COMPLETE = "trial:gwenith-complete";

    @Getter
    private final Set<String> verifiedPorts;
    @Getter
    private final Set<String> verifiedActivities;
    @Getter
    private final RecommendationConfidence confidence;

    public SailingSnapshot(
            Set<String> verifiedPorts,
            Set<String> verifiedActivities,
            RecommendationConfidence confidence)
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
                ? RecommendationConfidence.CHECK_NEEDED
                : confidence;
    }

    public static SailingSnapshot unknown()
    {
        return new SailingSnapshot(
                Collections.emptySet(),
                Collections.emptySet(),
                RecommendationConfidence.CHECK_NEEDED
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
