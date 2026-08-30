package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/** Applies live account and plan-relative inventory evidence before ranking. */
@Singleton
public final class MethodStrategyService
{
    public MethodStrategyAssessment assess(StrategyDataBundle data,
            MethodStrategyProfile profile)
    {
        if (data == null || data.getAccount() == null)
            return new MethodStrategyAssessment(profile != null, 0.0,
                    profile == null ? "No verified strategy profile applies."
                            : profile.getPlayerReason());
        if (profile == null)
            return new MethodStrategyAssessment(false, 0.0,
                    "No verified strategy profile applies to this account.");
        AccountMode mode = AccountMode.fromTypeCode(
                data.getAccount().getAccountTypeCode());
        if (!profile.supports(mode))
            return new MethodStrategyAssessment(false, 0.0,
                    "The sourced strategy does not apply to this account mode.");
        if (mode == AccountMode.ULTIMATE_IRONMAN
                && profile.getBankingBehavior()
                        == MethodBankingBehavior.CONVENTIONAL_BANK_LOOP)
            return new MethodStrategyAssessment(false, 0.0,
                    "The route requires conventional banking.");

        MethodInventoryFootprint footprint = profile.getInventoryFootprint();
        InventorySnapshot inventory = data.getInventory();
        int occupied = UimSetupCostService.occupiedInventorySlots(inventory);
        int free = Math.max(0, 28 - occupied);
        if (mode == AccountMode.ULTIMATE_IRONMAN
                && inventory != null
                && inventory.hasCompleteSlotObservation()
                && free < footprint.getMinimumPracticalFreeSlots())
            return new MethodStrategyAssessment(false, 0.0,
                    "This method needs at least "
                            + footprint.getMinimumPracticalFreeSlots()
                            + " practical free inventory slots; " + free
                            + " are currently observed.");

        double score = profile.getAccountValueFit() * 8.0;
        if (mode == AccountMode.ULTIMATE_IRONMAN && inventory != null
                && inventory.hasCompleteSlotObservation())
        {
            int margin = free - footprint.getMinimumPracticalFreeSlots();
            if (margin <= 1) score -= 5.0;
            if (footprint.tearsDownCurrentSetup()) score -= 8.0;
            if (footprint.getFlow()
                    == InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS) score -= 3.0;
        }
        return new MethodStrategyAssessment(true, score,
                profile.getPlayerReason());
    }
}
