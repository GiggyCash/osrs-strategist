package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/**
 * Derives property-first strategy priorities from mode mechanics and observed
 * account state. It does not name or select training methods.
 */
@Singleton
public final class AccountStrategicPriorityService
{
    public AccountStrategicPriorityProfile assess(StrategyContext context)
    {
        if (context == null)
            return assess(AccountMode.UNKNOWN, null, false);
        return assess(context.getAccountMode(), context.getData(),
                context.isUseGroupStorage());
    }

    public AccountStrategicPriorityProfile assess(AccountMode requestedMode,
            StrategyDataBundle data, boolean useGroupStorage)
    {
        AccountMode mode = requestedMode == null
                ? AccountMode.UNKNOWN : requestedMode;
        EnumMap<AccountStrategicDimension, AccountStrategicPriority> result =
                new EnumMap<>(AccountStrategicDimension.class);

        if (mode == AccountMode.UNKNOWN)
        {
            unknown(result);
            return new AccountStrategicPriorityProfile(mode, result);
        }

        boolean ge = AccountModePolicy.mayUseGrandExchange(mode);
        boolean selfSource = AccountModePolicy.requiresSelfSourcing(mode);
        boolean uim = AccountModePolicy.requiresCapabilityCheckedStorage(mode);
        boolean group = mode.isGroupIronman();
        boolean hardcore = mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN;

        InventorySnapshot inventory = data == null ? null : data.getInventory();
        int occupied = inventory == null
                || !inventory.hasCompleteSlotObservation() ? -1
                : UimSetupCostService.occupiedInventorySlots(inventory);
        put(result, AccountStrategicDimension.INVENTORY_PRESSURE,
                uim ? occupied >= 24 ? StrategicPriority.CRITICAL
                        : StrategicPriority.HIGH : StrategicPriority.LOW,
                occupied < 0 ? RecommendationConfidence.CHECK_NEEDED
                        : RecommendationConfidence.VERIFIED,
                uim ? occupied < 0
                        ? "UIM cannot bank conventionally; current inventory pressure is not observed."
                        : "UIM cannot bank conventionally; " + occupied
                                + " occupied inventory stacks are observed."
                        : "A conventional bank is legal, so inventory pressure is normally temporary.");
        put(result, AccountStrategicDimension.BANK_AVAILABILITY,
                uim ? StrategicPriority.CRITICAL : StrategicPriority.LOW,
                uim ? CapabilityState.BLOCKED : CapabilityState.VERIFIED,
                RecommendationConfidence.VERIFIED,
                uim ? "Conventional bank-dependent routes are illegal for UIM."
                        : "This mode may use its conventional personal bank.");
        put(result, AccountStrategicDimension.GRAND_EXCHANGE_AVAILABILITY,
                ge || selfSource ? StrategicPriority.HIGH : StrategicPriority.LOW,
                ge ? CapabilityState.VERIFIED : CapabilityState.BLOCKED,
                RecommendationConfidence.VERIFIED,
                ge ? "Tradeable purchases are a legal substitute when price and affordability are verified."
                        : "Grand Exchange acquisition is unavailable; executable routes must not depend on it.");
        put(result, AccountStrategicDimension.SELF_SOURCING_BURDEN,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.HIGH
                        : StrategicPriority.LOW,
                RecommendationConfidence.VERIFIED,
                selfSource ? "This mode must source its own tradeable resources."
                        : "Tradeable shortfalls may be bought when doing so is worthwhile.");

        GroupStorageSnapshot groupStorage = data == null
                ? null : data.getGroupStorage();
        boolean freshGroupStorage = group && useGroupStorage
                && groupStorage != null && groupStorage.isObserved();
        RecommendationConfidence groupConfidence = freshGroupStorage
                ? RecommendationConfidence.VERIFIED
                : RecommendationConfidence.CHECK_NEEDED;
        put(result, AccountStrategicDimension.SHARED_RESOURCE_VALUE,
                freshGroupStorage ? StrategicPriority.HIGH
                        : StrategicPriority.NONE,
                freshGroupStorage ? CapabilityState.VERIFIED
                        : group ? CapabilityState.UNKNOWN
                        : CapabilityState.BLOCKED,
                group ? groupConfidence : RecommendationConfidence.VERIFIED,
                freshGroupStorage
                        ? "Fresh, enabled Group Storage evidence can satisfy shared resource shortfalls."
                        : group ? "Shared resources cannot be counted without fresh, enabled Group Storage evidence."
                        : "This account mode has no Group Storage.");
        put(result, AccountStrategicDimension.SHARED_INFRASTRUCTURE_VALUE,
                StrategicPriority.NONE,
                group ? CapabilityState.UNKNOWN : CapabilityState.BLOCKED,
                group ? RecommendationConfidence.CHECK_NEEDED
                        : RecommendationConfidence.VERIFIED,
                group ? "No reliable teammate infrastructure snapshot is available, so shared POH or unlock value is not counted."
                        : "This account mode has no group infrastructure to count.");
        put(result, AccountStrategicDimension.STORAGE_VALUE,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.MODERATE
                        : StrategicPriority.LOW,
                RecommendationConfidence.VERIFIED,
                uim ? "Verified legal storage changes inventory capacity and future setup cost for UIM."
                        : selfSource ? "Long-lived self-sourced items make useful storage moderately valuable."
                        : "Bank access makes specialised storage useful but usually substitutable.");
        put(result, AccountStrategicDimension.POH_VALUE,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.HIGH
                        : StrategicPriority.MODERATE,
                RecommendationConfidence.VERIFIED,
                uim ? "Personal POH storage, transport, and setup reuse can change the whole UIM route."
                        : selfSource ? "Personal POH utility is a durable self-sufficient unlock."
                        : "A personal POH is useful, while public-house access is only a possible substitute, never assumed live.");
        put(result, AccountStrategicDimension.TELEPORT_INFRASTRUCTURE_VALUE,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.HIGH
                        : StrategicPriority.MODERATE,
                RecommendationConfidence.VERIFIED,
                uim ? "Reusable transport reduces repeated travel and inventory setup on UIM."
                        : selfSource ? "Reusable transport reduces self-sourced consumables and repeated travel."
                        : "Transport saves time, but tradeable teleports may sometimes substitute.");
        put(result, AccountStrategicDimension.SETUP_COST_SENSITIVITY,
                uim ? StrategicPriority.CRITICAL
                        : hardcore ? StrategicPriority.HIGH
                        : StrategicPriority.MODERATE,
                RecommendationConfidence.VERIFIED,
                uim ? "Dismantling and rebuilding a UIM setup can require retrieval and inventory churn."
                        : hardcore ? "Risk-safe preparation and recovery make setup cost important."
                        : "Setup time should be weighed against session and activity value.");
        put(result, AccountStrategicDimension.DEATH_RISK_SENSITIVITY,
                hardcore ? StrategicPriority.CRITICAL
                        : uim ? StrategicPriority.HIGH
                        : StrategicPriority.MODERATE,
                RecommendationConfidence.VERIFIED,
                hardcore ? "A dangerous route can permanently end Hardcore status."
                        : uim ? "Death can disrupt or endanger retrieval-based UIM storage and setup."
                        : "Death and recovery cost still matter, but do not carry Hardcore status loss.");
        put(result, AccountStrategicDimension.CONSUMABLE_REPLACEMENT_DIFFICULTY,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.HIGH
                        : StrategicPriority.LOW,
                RecommendationConfidence.VERIFIED,
                selfSource ? "Consumed supplies must be replaced through self-sourcing or verified shared stock."
                        : "Tradeable supplies can often be replaced if price and liquidity support it.");
        put(result, AccountStrategicDimension.STORABLE_EQUIPMENT_VALUE,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.MODERATE
                        : StrategicPriority.LOW,
                RecommendationConfidence.VERIFIED,
                uim ? "A useful item that can be stored and safely retrieved may save permanent inventory pressure."
                        : "Conventional bank access limits the extra value of specialised equipment storage.");
        put(result, AccountStrategicDimension.DUPLICATE_GRIND_PENALTY,
                freshGroupStorage ? StrategicPriority.HIGH
                        : StrategicPriority.NONE,
                group ? groupConfidence : RecommendationConfidence.VERIFIED,
                freshGroupStorage
                        ? "Observed shared stock can prove that repeating the same acquisition would duplicate group work."
                        : group ? "No fresh shared evidence proves that a grind would be duplicated."
                        : "There is no group-shared progression to duplicate.");
        put(result, AccountStrategicDimension.GP_LIQUIDITY_STORAGE_VALUE,
                uim ? StrategicPriority.HIGH
                        : selfSource ? StrategicPriority.MODERATE
                        : StrategicPriority.LOW,
                RecommendationConfidence.VERIFIED,
                uim ? "Coins are compact, but any proposed long-term storage or conversion must be mechanically verified."
                        : selfSource ? "Self-sourced GP has meaningful replacement and opportunity value."
                        : "GE purchasing makes spendable liquidity useful, while normal banking handles storage.");

        return new AccountStrategicPriorityProfile(mode, result);
    }

    private static void unknown(
            Map<AccountStrategicDimension, AccountStrategicPriority> values)
    {
        for (AccountStrategicDimension dimension
                : AccountStrategicDimension.values())
        {
            StrategicPriority priority = dimension
                    == AccountStrategicDimension.BANK_AVAILABILITY
                    || dimension
                    == AccountStrategicDimension.GRAND_EXCHANGE_AVAILABILITY
                    || dimension
                    == AccountStrategicDimension.SELF_SOURCING_BURDEN
                    ? StrategicPriority.CRITICAL : StrategicPriority.NONE;
            put(values, dimension, priority,
                    CapabilityState.UNKNOWN,
                    RecommendationConfidence.CHECK_NEEDED,
                    "Account mode is unknown; restricted capabilities must fail closed until verified.");
        }
    }

    private static void put(
            Map<AccountStrategicDimension, AccountStrategicPriority> values,
            AccountStrategicDimension dimension,
            StrategicPriority priority,
            RecommendationConfidence confidence,
            String reason)
    {
        put(values, dimension, priority, CapabilityState.VERIFIED, confidence,
                reason);
    }

    private static void put(
            Map<AccountStrategicDimension, AccountStrategicPriority> values,
            AccountStrategicDimension dimension,
            StrategicPriority priority,
            CapabilityState capabilityState,
            RecommendationConfidence confidence,
            String reason)
    {
        values.put(dimension, new AccountStrategicPriority(dimension,
                priority, capabilityState, confidence, reason));
    }
}
