package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Enforces the UIM inventory hierarchy without ever inventing a droppable item.
 * A caller supplies item-specific compatibility and plan value; static storage
 * possibility alone is insufficient.
 */
@Singleton
public final class UimInventoryResolutionService
{
    private final UimCapabilityService capabilityService;

    @Inject
    public UimInventoryResolutionService(UimCapabilityService capabilityService)
    {
        this.capabilityService = capabilityService == null
                ? new UimCapabilityService() : capabilityService;
    }

    public UimInventoryResolutionService()
    {
        this(new UimCapabilityService());
    }

    public UimInventoryResolution resolve(GameData data,
            MethodInventoryFootprint footprint,
            boolean goodLowFootprintAlternativeKnown,
            boolean productiveConsumptionKnown,
            List<UimStorageOption> proposedStorage)
    {
        AccountMode mode = data == null || data.account() == null
                ? AccountMode.UNKNOWN : AccountMode.fromTypeCode(
                        data.account().getAccountTypeCode());
        if (mode != AccountMode.ULTIMATE_IRONMAN)
            return unresolved(get(997));

        var inventory = data.inventory();
        if (inventory == null || !inventory.hasCompleteSlotObservation())
            return unresolved(get(999));
        MethodInventoryFootprint needed = footprint == null
                ? MethodInventoryFootprint.lowPressure() : footprint;
        int free = Math.max(0, 28
                - UimSetupCostService.occupiedInventorySlots(inventory));
        if (free >= needed.getMinimumPracticalFreeSlots())
            return result(UimInventoryResolutionKind.USE_AS_IS,
                    Confidence.VERIFIED, null, null,
                    get(1000));
        if (goodLowFootprintAlternativeKnown)
            return result(UimInventoryResolutionKind.USE_LOW_FOOTPRINT_ALTERNATIVE,
                    Confidence.VERIFIED, null, null,
                    get(1001));
        if (productiveConsumptionKnown)
            return result(UimInventoryResolutionKind.PRODUCTIVELY_CONSUME_RESOURCES,
                    Confidence.CHECK_NEEDED, null, null,
                    get(1002));

        List<UimStorageOption> options = proposedStorage == null
                ? Collections.emptyList() : new ArrayList<>(proposedStorage);
        options.sort(Comparator.comparingInt(
                option -> priority(option.getCapability())));

        for (UimStorageOption option : options)
        {
            var capability = option.getCapability();
            if (option.isRequiresConstruction()
                    || UimStorageMechanics.isRestrictedRetrieval(capability))
                continue;
            var decision = evaluate(data, option);
            if (decision.isAllowed())
                return result(UimInventoryResolutionKind.USE_VERIFIED_SAFE_STORAGE,
                        Confidence.VERIFIED, decision, null,
                        get(1003));
        }

        for (UimStorageOption option : options)
        {
            if (!option.isRequiresConstruction()
                    || UimStorageMechanics.isRestrictedRetrieval(
                            option.getCapability())
                    || option.getRecurringInfrastructureValue().ordinal()
                            < StrategicPriority.HIGH.ordinal()) continue;
            return result(UimInventoryResolutionKind.BUILD_HIGH_VALUE_SAFE_STORAGE,
                    Confidence.CHECK_NEEDED, null, null,
                    get(1004));
        }

        for (UimStorageOption option : options)
        {
            if (option.getCapability() != StorageCapability.LOOTING_BAG)
                continue;
            var decision = evaluate(data, option);
            if (decision.isAllowed())
                return result(UimInventoryResolutionKind.USE_RESTRICTED_RETRIEVAL,
                        Confidence.CHECK_NEEDED, decision, null,
                        get(1005));
        }

        for (UimStorageOption option : options)
        {
            var capability = option.getCapability();
            if (!UimStorageMechanics.isDangerous(capability)
                    || !option.isMajorProgressionTransition()) continue;
            var decision = evaluate(data, option);
            if (decision.isAllowed())
                return result(UimInventoryResolutionKind.USE_DANGEROUS_DEATH_STORAGE,
                        Confidence.CHECK_NEEDED, decision,
                        RecommendationRiskDisclosure.deathStorage(),
                        get(1006));
        }
        return unresolved(get(998));
    }

    private UimStorageDecision evaluate(GameData data,
            UimStorageOption option)
    {
        return capabilityService.evaluateStorage(data, option.getCapability(),
                option.getItemCompatibility(),
                option.getCapacityOrPreconditions());
    }

    private static int priority(StorageCapability capability)
    {
        if (capability == StorageCapability.POH_COSTUME_ROOM
                || capability == StorageCapability.POH_STORAGE
                || capability == StorageCapability.STASH
                || capability == StorageCapability.TOOL_LEPRECHAUN) return 0;
        if (capability == StorageCapability.SEED_BOX
                || capability == StorageCapability.HERB_SACK
                || capability == StorageCapability.RUNE_POUCH) return 1;
        if (capability == StorageCapability.LOOTING_BAG) return 2;
        return 3;
    }

    private static UimInventoryResolution unresolved(String reason)
    {
        return result(UimInventoryResolutionKind.UNRESOLVED,
                Confidence.CHECK_NEEDED, null, null, reason);
    }

    private static UimInventoryResolution result(
            UimInventoryResolutionKind kind,
            Confidence confidence,
            UimStorageDecision decision,
            RecommendationRiskDisclosure disclosure, String reason)
    {
        return new UimInventoryResolution(kind, confidence, decision,
                disclosure, reason);
    }
}
