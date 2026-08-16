package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

/**
 * Emits account-mode constraints as internal strategy signals.
 *
 * <p>These signals are mostly guardrails. They make mode restrictions visible
 * to the reasoning pipeline so future economy/resource modules cannot forget
 * that a Main, Ironman, GIM, HCIM, and UIM need different routes.</p>
 */
@Singleton
public class AccountModeStrategyModule implements StrategyModule
{
    @Override
    public String getId()
    {
        return "account-mode";
    }

    @Override
    public List<StrategySignal> analyze(StrategyContext context)
    {
        List<StrategySignal> signals = new ArrayList<>();
        AccountMode mode = context.getAccountMode();

        if (mode == AccountMode.UNKNOWN)
        {
            signals.add(
                    new StrategySignal(
                            "account-mode:unknown",
                            StrategySignalCategory.ACCOUNT_MODE,
                            "Account mode needs verification",
                            0.0,
                            RecommendationConfidence.CHECK_NEEDED
                    )
            );
            return signals;
        }

        if (AccountModePolicy.requiresCapabilityCheckedStorage(mode))
        {
            signals.add(
                    new StrategySignal(
                            "account-mode:uim-storage",
                            StrategySignalCategory.STORAGE,
                            "UIM storage routes require verified capabilities",
                            0.0,
                            RecommendationConfidence.VERIFIED
                    )
            );
        }

        if (AccountModePolicy.mayUseGroupStorage(
                mode,
                context.isUseGroupStorage()))
        {
            signals.add(
                    new StrategySignal(
                            "account-mode:group-storage",
                            StrategySignalCategory.STORAGE,
                            "Group Storage may be used when observed",
                            0.0,
                            RecommendationConfidence.CHECK_NEEDED
                    )
            );
        }

        return signals;
    }
}
