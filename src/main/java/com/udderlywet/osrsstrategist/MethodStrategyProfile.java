package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;

/** Sourced strategic properties layered over a mechanically legal method. */
public final class MethodStrategyProfile
{
    @Getter
    private final String methodId;
    @Getter
    private final StrategyKnowledgeTier tier;
    @Getter
    private final Set<AccountMode> accountModes;
    @Getter
    private final MethodBankingBehavior bankingBehavior;
    @Getter
    private final MethodInventoryFootprint inventoryFootprint;
    @Getter
    private final double accountValueFit;
    @Getter
    private final String playerReason;
    @Getter
    private final List<StrategySourceId> sources;

    public MethodStrategyProfile(String methodId, StrategyKnowledgeTier tier,
            Set<AccountMode> accountModes,
            MethodBankingBehavior bankingBehavior,
            MethodInventoryFootprint inventoryFootprint,
            double accountValueFit, String playerReason,
            List<StrategySourceId> sources)
    {
        this.methodId = methodId;
        this.tier = tier;
        this.accountModes = accountModes == null || accountModes.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(accountModes));
        this.bankingBehavior = bankingBehavior == null
                ? MethodBankingBehavior.UNKNOWN : bankingBehavior;
        this.inventoryFootprint = inventoryFootprint == null
                ? MethodInventoryFootprint.lowPressure() : inventoryFootprint;
        this.accountValueFit = Math.max(-1.0, Math.min(1.0,
                accountValueFit));
        this.playerReason = playerReason;
        this.sources = Collections.unmodifiableList(sources == null
                ? new ArrayList<>() : new ArrayList<>(sources));
    }

    public boolean supports(AccountMode mode) { return accountModes.contains(mode); }
}
