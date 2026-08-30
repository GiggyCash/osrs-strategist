package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Immutable result of resolving a deterministic material list against the
 * account state Compass has actually observed.
 */
public final class AccountResourcePlan
{
    @Getter
    private final AccountMode accountMode;
    @Getter
    private final boolean primaryStorageObserved;
    @Getter
    private final boolean groupStorageIncluded;
    @Getter
    private final boolean groupStorageObserved;
    @Getter
    private final List<ResourcePlanEntry> entries;
    @Getter
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
