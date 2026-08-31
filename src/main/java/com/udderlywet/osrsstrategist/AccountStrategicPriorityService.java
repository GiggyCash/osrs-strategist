package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

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
        return assess(context.accountMode(), context.data(),
                context.isUseGroupStorage());
    }

    public AccountStrategicPriorityProfile assess(AccountMode requestedMode,
            GameData data, boolean useGroupStorage)
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

        ItemsState inventory = data == null ? null : data.inventory();
        int occupied = inventory == null
                || !inventory.hasCompleteSlotObservation() ? -1
                : UimSetupCostService.occupiedInventorySlots(inventory);
        put(result, AccountStrategicDimension.INVENTORY_PRESSURE,
                uim ? occupied >= 24 ? StrategicPriority.CRITICAL
                        : StrategicPriority.HIGH : StrategicPriority.LOW,
                occupied < 0 ? Confidence.CHECK_NEEDED
                        : Confidence.VERIFIED,
                uim ? occupied < 0
                        ? get(89)
                        : get(1434) + occupied
                                + get(100)
                        : get(111));
        put(result, AccountStrategicDimension.BANK_AVAILABILITY,
                uim ? StrategicPriority.CRITICAL : StrategicPriority.LOW,
                uim ? CapabilityState.BLOCKED : CapabilityState.VERIFIED,
                Confidence.VERIFIED,
                uim ? get(122)
                        : get(124));
        put(result, AccountStrategicDimension.GRAND_EXCHANGE_AVAILABILITY,
                ge || selfSource ? StrategicPriority.HIGH : StrategicPriority.LOW,
                ge ? CapabilityState.VERIFIED : CapabilityState.BLOCKED,
                Confidence.VERIFIED,
                ge ? get(125)
                        : get(126));
        put(result, AccountStrategicDimension.SELF_SOURCING_BURDEN,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.HIGH
                        : StrategicPriority.LOW,
                Confidence.VERIFIED,
                selfSource ? get(127)
                        : get(128));

        ItemsState groupStorage = data == null
                ? null : data.groupStorage();
        boolean freshGroupStorage = group && useGroupStorage
                && groupStorage != null && groupStorage.isObserved();
        Confidence groupConfidence = freshGroupStorage
                ? Confidence.VERIFIED
                : Confidence.CHECK_NEEDED;
        put(result, AccountStrategicDimension.SHARED_RESOURCE_VALUE,
                freshGroupStorage ? StrategicPriority.HIGH
                        : StrategicPriority.NONE,
                freshGroupStorage ? CapabilityState.VERIFIED
                        : group ? CapabilityState.UNKNOWN
                        : CapabilityState.BLOCKED,
                group ? groupConfidence : Confidence.VERIFIED,
                freshGroupStorage
                        ? get(90)
                        : group ? get(91)
                        : get(92));
        put(result, AccountStrategicDimension.SHARED_INFRASTRUCTURE_VALUE,
                StrategicPriority.NONE,
                group ? CapabilityState.UNKNOWN : CapabilityState.BLOCKED,
                group ? Confidence.CHECK_NEEDED
                        : Confidence.VERIFIED,
                group ? get(93)
                        : get(94));
        put(result, AccountStrategicDimension.STORAGE_VALUE,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.MODERATE
                        : StrategicPriority.LOW,
                Confidence.VERIFIED,
                uim ? get(95)
                        : selfSource ? get(96)
                        : get(97));
        put(result, AccountStrategicDimension.POH_VALUE,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.HIGH
                        : StrategicPriority.MODERATE,
                Confidence.VERIFIED,
                uim ? get(98)
                        : selfSource ? get(99)
                        : get(101));
        put(result, AccountStrategicDimension.TELEPORT_INFRASTRUCTURE_VALUE,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.HIGH
                        : StrategicPriority.MODERATE,
                Confidence.VERIFIED,
                uim ? get(102)
                        : selfSource ? get(103)
                        : get(104));
        put(result, AccountStrategicDimension.SETUP_COST_SENSITIVITY,
                uim ? StrategicPriority.CRITICAL
                        : hardcore ? StrategicPriority.HIGH
                        : StrategicPriority.MODERATE,
                Confidence.VERIFIED,
                uim ? get(105)
                        : hardcore ? get(106)
                        : get(107));
        put(result, AccountStrategicDimension.DEATH_RISK_SENSITIVITY,
                hardcore ? StrategicPriority.CRITICAL
                        : uim ? StrategicPriority.HIGH
                        : StrategicPriority.MODERATE,
                Confidence.VERIFIED,
                hardcore ? get(108)
                        : uim ? get(109)
                        : get(110));
        put(result, AccountStrategicDimension.CONSUMABLE_REPLACEMENT_DIFFICULTY,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.HIGH
                        : StrategicPriority.LOW,
                Confidence.VERIFIED,
                selfSource ? get(112)
                        : get(113));
        put(result, AccountStrategicDimension.STORABLE_EQUIPMENT_VALUE,
                uim ? StrategicPriority.CRITICAL
                        : selfSource ? StrategicPriority.MODERATE
                        : StrategicPriority.LOW,
                Confidence.VERIFIED,
                uim ? get(114)
                        : get(115));
        put(result, AccountStrategicDimension.DUPLICATE_GRIND_PENALTY,
                freshGroupStorage ? StrategicPriority.HIGH
                        : StrategicPriority.NONE,
                group ? groupConfidence : Confidence.VERIFIED,
                freshGroupStorage
                        ? get(116)
                        : group ? get(117)
                        : get(118));
        put(result, AccountStrategicDimension.GP_LIQUIDITY_STORAGE_VALUE,
                uim ? StrategicPriority.HIGH
                        : selfSource ? StrategicPriority.MODERATE
                        : StrategicPriority.LOW,
                Confidence.VERIFIED,
                uim ? get(119)
                        : selfSource ? get(120)
                        : get(121));

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
                    Confidence.CHECK_NEEDED,
                    get(123));
        }
    }

    private static void put(
            Map<AccountStrategicDimension, AccountStrategicPriority> values,
            AccountStrategicDimension dimension,
            StrategicPriority priority,
            Confidence confidence,
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
            Confidence confidence,
            String reason)
    {
        values.put(dimension, new AccountStrategicPriority(dimension,
                priority, capabilityState, confidence, reason));
    }
}
