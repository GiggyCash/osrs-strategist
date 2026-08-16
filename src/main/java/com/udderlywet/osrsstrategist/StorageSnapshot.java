package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Verified/unknown state for storage systems that affect planning.
 *
 * <p>The important design rule is that UNKNOWN remains different from
 * UNAVAILABLE. Strategist must never turn "we have not observed it" into
 * "the player does not have it".</p>
 */
public final class StorageSnapshot
{
    private final Map<StorageCapability, CapabilityState> states;

    public StorageSnapshot(Map<StorageCapability, CapabilityState> states)
    {
        EnumMap<StorageCapability, CapabilityState> copy =
                new EnumMap<>(StorageCapability.class);

        if (states != null)
        {
            copy.putAll(states);
        }

        this.states = Collections.unmodifiableMap(copy);
    }

    public static StorageSnapshot unknown()
    {
        return new StorageSnapshot(Collections.emptyMap());
    }

    public CapabilityState stateOf(StorageCapability capability)
    {
        return states.getOrDefault(
                capability,
                CapabilityState.UNKNOWN
        );
    }

    public boolean verified(StorageCapability capability)
    {
        return stateOf(capability) == CapabilityState.VERIFIED;
    }

    public Map<StorageCapability, CapabilityState> getStates()
    {
        return states;
    }
}
