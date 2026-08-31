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
                        ? PlayerText.get("ASPS1")
                        : "UIM cannot bank conventionally; " + occupied
                                + PlayerText.get("ASPS2")
                        : PlayerText.get("ASPS3"));
        put(result, AccountStrategicDimension.BANK_AVAILABILITY,
                uim ? StrategicPriority.CRITICAL : StrategicPriority.LOW,
                uim ? CapabilityState.BLOCKED : CapabilityState.VERIFIED,
                RecommendationConfidence.VERIFIED,
                uim ? PlayerText.get("ASPS4")
                        : PlayerText.get("ASPS5"));
        put(result, AccountStrategicDimension.GRAND_EXCHANGE_AVAILABILITY,
                ge || selfSource ? StrategicPriority.HIGH : StrategicPriority.LOW,
                ge ? CapabilityState.VERIFIED : CapabilityState.BLOCKED,
                RecommendationConfidence.VERIFIED,
                ge ? PlayerText.get("ASPS6")
                        : PlayerText.get("ASPS7"));
        put(result, AccountStrategicDimension.SELF_SOURCING_BURDEN,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.HIGH
                        : StrategicPriority.LOW,
                RecommendationConfidence.VERIFIED,
                selfSource ? PlayerText.get("ASPS8")
                        : PlayerText.get("ASPS9"));

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
                        ? PlayerText.get("ASPS10")
                        : group ? PlayerText.get("ASPS11")
                        : PlayerText.get("ASPS12"));
        put(result, AccountStrategicDimension.SHARED_INFRASTRUCTURE_VALUE,
                StrategicPriority.NONE,
                group ? CapabilityState.UNKNOWN : CapabilityState.BLOCKED,
                group ? RecommendationConfidence.CHECK_NEEDED
                        : RecommendationConfidence.VERIFIED,
                group ? PlayerText.get("ASPS13")
                        : PlayerText.get("ASPS14"));
        put(result, AccountStrategicDimension.STORAGE_VALUE,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.MODERATE
                        : StrategicPriority.LOW,
                RecommendationConfidence.VERIFIED,
                uim ? PlayerText.get("ASPS15")
                        : selfSource ? PlayerText.get("ASPS16")
                        : PlayerText.get("ASPS17"));
        put(result, AccountStrategicDimension.POH_VALUE,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.HIGH
                        : StrategicPriority.MODERATE,
                RecommendationConfidence.VERIFIED,
                uim ? PlayerText.get("ASPS18")
                        : selfSource ? PlayerText.get("ASPS19")
                        : PlayerText.get("ASPS20"));
        put(result, AccountStrategicDimension.TELEPORT_INFRASTRUCTURE_VALUE,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.HIGH
                        : StrategicPriority.MODERATE,
                RecommendationConfidence.VERIFIED,
                uim ? PlayerText.get("ASPS21")
                        : selfSource ? PlayerText.get("ASPS22")
                        : PlayerText.get("ASPS23"));
        put(result, AccountStrategicDimension.SETUP_COST_SENSITIVITY,
                uim ? StrategicPriority.CRITICAL
                        : hardcore ? StrategicPriority.HIGH
                        : StrategicPriority.MODERATE,
                RecommendationConfidence.VERIFIED,
                uim ? PlayerText.get("ASPS24")
                        : hardcore ? PlayerText.get("ASPS25")
                        : PlayerText.get("ASPS26"));
        put(result, AccountStrategicDimension.DEATH_RISK_SENSITIVITY,
                hardcore ? StrategicPriority.CRITICAL
                        : uim ? StrategicPriority.HIGH
                        : StrategicPriority.MODERATE,
                RecommendationConfidence.VERIFIED,
                hardcore ? PlayerText.get("ASPS27")
                        : uim ? PlayerText.get("ASPS28")
                        : PlayerText.get("ASPS29"));
        put(result, AccountStrategicDimension.CONSUMABLE_REPLACEMENT_DIFFICULTY,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.HIGH
                        : StrategicPriority.LOW,
                RecommendationConfidence.VERIFIED,
                selfSource ? PlayerText.get("ASPS30")
                        : PlayerText.get("ASPS31"));
        put(result, AccountStrategicDimension.STORABLE_EQUIPMENT_VALUE,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.MODERATE
                        : StrategicPriority.LOW,
                RecommendationConfidence.VERIFIED,
                uim ? PlayerText.get("ASPS32")
                        : PlayerText.get("ASPS33"));
        put(result, AccountStrategicDimension.DUPLICATE_GRIND_PENALTY,
                freshGroupStorage ? StrategicPriority.HIGH
                        : StrategicPriority.NONE,
                group ? groupConfidence : RecommendationConfidence.VERIFIED,
                freshGroupStorage
                        ? PlayerText.get("ASPS34")
                        : group ? PlayerText.get("ASPS35")
                        : PlayerText.get("ASPS36"));
        put(result, AccountStrategicDimension.GP_LIQUIDITY_STORAGE_VALUE,
                uim ? StrategicPriority.HIGH
                        : selfSource ? StrategicPriority.MODERATE
                        : StrategicPriority.LOW,
                RecommendationConfidence.VERIFIED,
                uim ? PlayerText.get("ASPS37")
                        : selfSource ? PlayerText.get("ASPS38")
                        : PlayerText.get("ASPS39"));

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
                    PlayerText.get("ASPS40"));
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
