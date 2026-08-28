package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Values a concrete gear upgrade from marginal benefit, replacement horizon,
 * acquisition, and account-mode storage properties. It never names a target.
 */
@Singleton
public final class ContextualGearValueService
{
    private final GearAcquisitionCatalog acquisition;

    @Inject
    public ContextualGearValueService(GearAcquisitionCatalog acquisition)
    {
        this.acquisition = acquisition;
    }

    public ContextualGearValueService()
    {
        this(new GearAcquisitionCatalog());
    }

    public ContextualGearValueAssessment assess(StrategyContext context,
            GearUpgradeValueRequest request)
    {
        if (context == null || context.getData() == null || request == null
                || request.getProgression() == null
                || request.getTargetItem() == null
                || request.getTargetItem().trim().isEmpty())
        {
            return result(GearUpgradeValueState.NEEDS_EVIDENCE, -3, null,
                    "An exact target and encounter context are required.");
        }
        GearProgressionEntry entry = request.getProgression();
        AccountSnapshot account = context.getData().getAccount();
        if (account == null)
            return result(GearUpgradeValueState.NEEDS_EVIDENCE, -3, null,
                    "Account mode and membership are unknown.");
        AccountMode mode = context.getAccountMode();
        if (!ContentAccessRules.isContentAvailable(
                account.getMembershipStatus(), entry.isFreeToPlay())
                || !AccountBuildPolicy.allowsGearEntry(account, entry)
                || (mode.isIronLike() && !entry.isSelfSourceFriendly())
                || (mode == AccountMode.ULTIMATE_IRONMAN && !entry.isUimFriendly())
                || ((mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                    && !entry.isHardcoreSafe()))
        {
            return result(GearUpgradeValueState.INELIGIBLE, -20, null,
                    "The progression entry is not legal for this account context.");
        }

        ObservedItemIndex items = new ObservedItemIndex(context.getData(),
                context.isUseGroupStorage());
        if (items.has(request.getTargetItem()))
            return result(GearUpgradeValueState.OWNED, 0,
                    acquisition.forItem(request.getTargetItem()),
                    "The exact target is already present in usable observed storage.");
        if (mode != AccountMode.ULTIMATE_IRONMAN && !items.bankObserved())
            return result(GearUpgradeValueState.NEEDS_EVIDENCE, -2,
                    acquisition.forItem(request.getTargetItem()),
                    "Open the bank before calling this exact item missing.");

        GearAcquisitionRoute route = acquisition.forItem(request.getTargetItem());
        if (route == null)
            return result(GearUpgradeValueState.NEEDS_EVIDENCE, -3, null,
                    "No audited acquisition route exists for this exact target.");

        int value = benefit(request.getMarginalBenefit())
                + horizon(request.getReplacementHorizon())
                - burden(request.getAcquisitionBurden());
        if (request.isProvenGoalRelevant()) value += 4;
        if (mode.usesGrandExchange() && route.isTradeable()) value += 2;
        if (mode.isIronLike() && request.getReplacementHorizon()
                == GearReplacementHorizon.LONG) value += 2;
        if (mode == AccountMode.ULTIMATE_IRONMAN)
            value += uimStorageValue(request.getStorageDisposition());
        value = Math.max(-14, Math.min(14, value));

        GearUpgradeValueState state = request.getMarginalBenefit()
                == GearMarginalBenefit.UNKNOWN
                || request.getAcquisitionBurden() == GearAcquisitionBurden.UNKNOWN
                || (mode == AccountMode.ULTIMATE_IRONMAN
                    && request.getStorageDisposition() == GearStorageDisposition.UNKNOWN)
                ? GearUpgradeValueState.NEEDS_EVIDENCE
                : value >= 4 ? GearUpgradeValueState.WORTH_CONSIDERING
                : GearUpgradeValueState.DEFER;
        String evidence = "Value derives from supplied encounter benefit ("
                + request.getMarginalBenefit() + "), replacement horizon ("
                + request.getReplacementHorizon() + "), acquisition burden ("
                + request.getAcquisitionBurden() + ") and storage consequence ("
                + request.getStorageDisposition() + ").";
        return result(state, value, route, evidence);
    }

    private static int benefit(GearMarginalBenefit value)
    {
        switch (value)
        {
            case MINOR: return 1;
            case MEANINGFUL: return 5;
            case MAJOR: return 9;
            case UNKNOWN:
            default: return 0;
        }
    }

    private static int horizon(GearReplacementHorizon value)
    {
        switch (value)
        {
            case SHORT: return -4;
            case MEDIUM: return 1;
            case LONG: return 4;
            case UNKNOWN:
            default: return 0;
        }
    }

    private static int burden(GearAcquisitionBurden value)
    {
        switch (value)
        {
            case LOW: return 1;
            case MODERATE: return 3;
            case HIGH: return 6;
            case VERY_HIGH: return 10;
            case UNKNOWN:
            default: return 0;
        }
    }

    private static int uimStorageValue(GearStorageDisposition value)
    {
        switch (value)
        {
            case VERIFIED_STORABLE: return 5;
            case EASILY_REOBTAINABLE: return 3;
            case OCCUPIES_PERSISTENT_SLOT: return -7;
            case RETRIEVAL_SETUP_REQUIRED: return -4;
            case UNKNOWN:
            default: return 0;
        }
    }

    private static ContextualGearValueAssessment result(
            GearUpgradeValueState state, int score, GearAcquisitionRoute route,
            String evidence)
    {
        return new ContextualGearValueAssessment(state, score, route, evidence);
    }
}
