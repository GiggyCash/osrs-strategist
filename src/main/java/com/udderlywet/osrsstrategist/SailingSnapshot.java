package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Sailing discovery/progression state.
 *
 * <p>Sailing is expected to evolve quickly. Keeping ports and activities as
 * data keys means the recommendation engine can gain new Sailing coverage by
 * updating structured game data instead of changing its core algorithm.</p>
 */
public final class SailingSnapshot
{
    private final Set<String> verifiedPorts;
    private final Set<String> verifiedActivities;
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

    public Set<String> getVerifiedPorts() { return verifiedPorts; }
    public Set<String> getVerifiedActivities() { return verifiedActivities; }
    public RecommendationConfidence getConfidence() { return confidence; }
}
