package compass;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

/** Sourced strategic properties layered over a mechanically legal method. */
@RequiredArgsConstructor
@Getter
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


    public boolean supports(AccountMode mode) { return accountModes.contains(mode); }
}
