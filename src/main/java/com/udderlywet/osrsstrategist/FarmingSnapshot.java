package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Farming-specific account state used by herb/tree/farming-contract planning.
 *
 * <p>Patch availability is stored as verified IDs. Tool Leprechaun contents
 * are kept separate because simply having access to a Leprechaun does not mean
 * every tool is stored there.</p>
 */
public final class FarmingSnapshot
{
    private final Set<String> reachablePatchIds;
    private final Map<String, CapabilityState> leprechaunTools;
    private final Map<String, Long> patchReadyAtMillis;

    public FarmingSnapshot(
            Set<String> reachablePatchIds,
            Map<String, CapabilityState> leprechaunTools,
            Map<String, Long> patchReadyAtMillis)
    {
        this.reachablePatchIds = Collections.unmodifiableSet(
                reachablePatchIds == null
                        ? new HashSet<>()
                        : new HashSet<>(reachablePatchIds)
        );
        this.leprechaunTools = Collections.unmodifiableMap(
                leprechaunTools == null
                        ? new HashMap<>()
                        : new HashMap<>(leprechaunTools)
        );
        this.patchReadyAtMillis = Collections.unmodifiableMap(
                patchReadyAtMillis == null
                        ? new HashMap<>()
                        : new HashMap<>(patchReadyAtMillis)
        );
    }

    public static FarmingSnapshot unknown()
    {
        return new FarmingSnapshot(
                Collections.emptySet(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    public boolean isPatchReachable(String patchId)
    {
        return patchId != null && reachablePatchIds.contains(patchId);
    }

    public CapabilityState leprechaunToolState(String toolId)
    {
        return leprechaunTools.getOrDefault(
                toolId,
                CapabilityState.UNKNOWN
        );
    }

    public Long readyAt(String patchId)
    {
        return patchReadyAtMillis.get(patchId);
    }

    public Set<String> getReachablePatchIds() { return reachablePatchIds; }
    public Map<String, CapabilityState> getLeprechaunTools() { return leprechaunTools; }
    public Map<String, Long> getPatchReadyAtMillis() { return patchReadyAtMillis; }
}
