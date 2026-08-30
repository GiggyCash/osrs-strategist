package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Sourced strategic properties layered over a mechanically legal method. */
public final class MethodStrategyProfile
{
    private final String methodId;
    private final StrategyKnowledgeTier tier;
    private final Set<AccountMode> accountModes;
    private final MethodBankingBehavior bankingBehavior;
    private final MethodInventoryFootprint inventoryFootprint;
    private final double accountValueFit;
    private final String playerReason;
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

    public String getMethodId() { return methodId; }
    public StrategyKnowledgeTier getTier() { return tier; }
    public boolean supports(AccountMode mode) { return accountModes.contains(mode); }
    public Set<AccountMode> getAccountModes() { return accountModes; }
    public MethodBankingBehavior getBankingBehavior() { return bankingBehavior; }
    public MethodInventoryFootprint getInventoryFootprint() { return inventoryFootprint; }
    public double getAccountValueFit() { return accountValueFit; }
    public String getPlayerReason() { return playerReason; }
    public List<StrategySourceId> getSources() { return sources; }
}
