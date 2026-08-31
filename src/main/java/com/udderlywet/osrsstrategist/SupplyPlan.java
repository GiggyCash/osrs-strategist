package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/**
 * Immutable result of resolving a deterministic material list against the
 * account state Compass has actually observed.
 */
@Getter
public final class SupplyPlan
{
    private final AccountMode accountMode;
    private final boolean primaryStorageObserved;
    private final boolean groupStorageIncluded;
    private final boolean groupStorageObserved;
    private final List<ResourcePlanEntry> entries;
    private final String guidance;

    public SupplyPlan(
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

    public AccountMode accountMode() { return accountMode; }

    public int getTotalMissingUnits()
    {
        var total = 0L;
        for (ResourcePlanEntry entry : entries)
        {
            total += entry.getMissing();
            if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    public List<MethodInput> getMissingInputs()
    {
        List<MethodInput> result = new ArrayList<>();
        for (ResourcePlanEntry entry : entries)
        {
            if (entry.getMissing() > 0) result.add(entry.missingInput());
        }
        return Collections.unmodifiableList(result);
    }
}
