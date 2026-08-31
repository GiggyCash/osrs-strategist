package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/**
 * Generic cooldown/ready-time storage for recurring OSRS activities.
 *
 * <p>Birdhouses, herb/tree runs, Tears of Guthix, Kingdom, farming contracts,
 * daily diary rewards, and future cooldown content should all flow through
 * this single model rather than each feature inventing its own timer system.</p>
 */
public final class RecurringOpportunitySnapshot
{
    @Getter
    private final Map<String, Long> readyAtMillis;

    public RecurringOpportunitySnapshot(Map<String, Long> readyAtMillis)
    {
        this.readyAtMillis = Collections.unmodifiableMap(
                readyAtMillis == null
                        ? new HashMap<>()
                        : new HashMap<>(readyAtMillis)
        );
    }

    public static RecurringOpportunitySnapshot unknown()
    {
        return new RecurringOpportunitySnapshot(Collections.emptyMap());
    }

    public Long readyAt(String opportunityId)
    {
        return readyAtMillis.get(opportunityId);
    }

    public boolean isReadyNow(String opportunityId, long nowMillis)
    {
        Long readyAt = readyAtMillis.get(opportunityId);
        return readyAt != null && readyAt <= nowMillis;
    }

}
