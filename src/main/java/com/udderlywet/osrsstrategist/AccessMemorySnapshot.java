package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/**
 * Per-character memory of places/capabilities Compass has directly observed.
 *
 * <p>The value is the last observed wall-clock time in milliseconds. Remembering
 * positive evidence lets the planner stop repeatedly asking whether an account
 * can reach an area it has already seen the player use.</p>
 */
public final class AccessMemorySnapshot
{
    @Getter
    private final Map<String, Long> lastObservedAtMillis;

    public AccessMemorySnapshot(Map<String, Long> values)
    {
        this.lastObservedAtMillis = Collections.unmodifiableMap(
                values == null
                        ? new HashMap<>()
                        : new HashMap<>(values)
        );
    }

    public static AccessMemorySnapshot empty()
    {
        return new AccessMemorySnapshot(Collections.emptyMap());
    }

    public boolean hasObserved(String key)
    {
        return key != null && lastObservedAtMillis.containsKey(key);
    }

    public Long lastObservedAt(String key)
    {
        return lastObservedAtMillis.get(key);
    }

}
