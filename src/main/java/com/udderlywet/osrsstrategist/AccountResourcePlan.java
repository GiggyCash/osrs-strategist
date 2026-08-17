package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result of resolving a deterministic material list against the
 * account state Strategist has actually observed.
 */
public final class AccountResourcePlan
{
    private final AccountMode accountMode;
    private final boolean primaryStorageObserved;
    private final boolean groupStorageIncluded;
    private final boolean groupStorageObserved;
    private final List<ResourcePlanEntry> entries;
    private final String guidance;

    public AccountResourcePlan(
            AccountMode accountMode,
            boolean primaryStorageObserved,
            boolean groupStorageIncluded,
            boolean groupStorageObserved,
            List<ResourcePlanEntry> entries,
            String guidance)
    {
        this.accountMode = accountMode == null ? AccountMode.UNKNOWN : accountMode;
        this.primaryStorageObserved = primaryStorageObserved;
        this.groupStorageIncluded = groupStorageIncluded;
        this.groupStorageObserved = groupStorageObserved;
        this.entries = Collections.unmodifiableList(entries == null
                ? new ArrayList<>() : new ArrayList<>(entries));
        this.guidance = guidance;
    }

    public AccountMode getAccountMode() { return accountMode; }
    public boolean isPrimaryStorageObserved() { return primaryStorageObserved; }
    public boolean isGroupStorageIncluded() { return groupStorageIncluded; }
    public boolean isGroupStorageObserved() { return groupStorageObserved; }
    public List<ResourcePlanEntry> getEntries() { return entries; }
    public String getGuidance() { return guidance; }

    public boolean isFullySupplied()
    {
        if (!primaryStorageObserved && accountMode != AccountMode.ULTIMATE_IRONMAN)
        {
            return false;
        }
        for (ResourcePlanEntry entry : entries)
        {
            if (!entry.isSatisfied()) return false;
        }
        return true;
    }

    public int getTotalMissingUnits()
    {
        long total = 0L;
        for (ResourcePlanEntry entry : entries)
        {
            total += entry.getMissing();
            if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    public List<ResolvedMethodInput> getMissingInputs()
    {
        List<ResolvedMethodInput> result = new ArrayList<>();
        for (ResourcePlanEntry entry : entries)
        {
            if (entry.getMissing() > 0) result.add(entry.missingInput());
        }
        return Collections.unmodifiableList(result);
    }
}
