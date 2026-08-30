package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;

/** Sourced strategic properties shared by non-skill candidate families. */
public final class ActivityStrategyProfile
{
    @Getter
    private final String candidatePrefix;
    private final Set<AccountMode> accountModes;
    @Getter
    private final MethodInventoryFootprint inventoryFootprint;
    @Getter
    private final double setupReuse;
    @Getter
    private final String strategicReason;
    @Getter
    private final List<StrategySourceId> sources;

    public ActivityStrategyProfile(String candidatePrefix,
            Set<AccountMode> accountModes,
            MethodInventoryFootprint inventoryFootprint,
            double setupReuse, String strategicReason,
            List<StrategySourceId> sources)
    {
        this.candidatePrefix = candidatePrefix;
        this.accountModes = accountModes == null || accountModes.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(accountModes));
        this.inventoryFootprint = inventoryFootprint;
        this.setupReuse = Math.max(0.0, Math.min(1.0, setupReuse));
        this.strategicReason = strategicReason;
        this.sources = Collections.unmodifiableList(sources == null
                ? new ArrayList<>() : new ArrayList<>(sources));
    }

    public boolean supports(AccountMode mode) { return accountModes.contains(mode); }
}
