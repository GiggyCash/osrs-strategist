package com.udderlywet.osrsstrategist;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

/** Sourced strategic properties shared by non-skill candidate families. */
@RequiredArgsConstructor
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


    public boolean supports(AccountMode mode) { return accountModes.contains(mode); }
}
