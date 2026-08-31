package com.udderlywet.osrsstrategist;

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

    public UimInventoryResolution resolve(StrategyDataBundle data,
            MethodInventoryFootprint footprint,
            boolean goodLowFootprintAlternativeKnown,
            boolean productiveConsumptionKnown,
            List<UimStorageOption> proposedStorage)
    {
        AccountMode mode = data == null || data.getAccount() == null
                ? AccountMode.UNKNOWN : AccountMode.fromTypeCode(
                        data.getAccount().getAccountTypeCode());
        if (mode != AccountMode.ULTIMATE_IRONMAN)
            return unresolved(PlayerText.get("UIRS1"));

        InventorySnapshot inventory = data.getInventory();
        if (inventory == null || !inventory.hasCompleteSlotObservation())
            return unresolved(PlayerText.get("UIRS2"));
        MethodInventoryFootprint needed = footprint == null
                ? MethodInventoryFootprint.lowPressure() : footprint;
        int free = Math.max(0, 28
                - UimSetupCostService.occupiedInventorySlots(inventory));
        if (free >= needed.getMinimumPracticalFreeSlots())
            return result(UimInventoryResolutionKind.USE_AS_IS,
                    RecommendationConfidence.VERIFIED, null, null,
                    PlayerText.get("UIRS3"));
        if (goodLowFootprintAlternativeKnown)
            return result(UimInventoryResolutionKind.USE_LOW_FOOTPRINT_ALTERNATIVE,
                    RecommendationConfidence.VERIFIED, null, null,
                    PlayerText.get("UIRS4"));
        if (productiveConsumptionKnown)
            return result(UimInventoryResolutionKind.PRODUCTIVELY_CONSUME_RESOURCES,
                    RecommendationConfidence.CHECK_NEEDED, null, null,
                    PlayerText.get("UIRS5"));

        List<UimStorageOption> options = proposedStorage == null
                ? Collections.emptyList() : new ArrayList<>(proposedStorage);
        options.sort(Comparator.comparingInt(
                option -> priority(option.getCapability())));

        for (UimStorageOption option : options)
        {
            StorageCapability capability = option.getCapability();
            if (option.isRequiresConstruction()
                    || UimStorageMechanics.isRestrictedRetrieval(capability))
                continue;
            UimStorageDecision decision = evaluate(data, option);
            if (decision.isAllowed())
                return result(UimInventoryResolutionKind.USE_VERIFIED_SAFE_STORAGE,
                        RecommendationConfidence.VERIFIED, decision, null,
                        PlayerText.get("UIRS6"));
        }

        for (UimStorageOption option : options)
        {
            if (!option.isRequiresConstruction()
                    || UimStorageMechanics.isRestrictedRetrieval(
                            option.getCapability())
                    || option.getRecurringInfrastructureValue().ordinal()
                            < StrategicPriority.HIGH.ordinal()) continue;
            return result(UimInventoryResolutionKind.BUILD_HIGH_VALUE_SAFE_STORAGE,
                    RecommendationConfidence.CHECK_NEEDED, null, null,
                    PlayerText.get("UIRS7"));
        }

        for (UimStorageOption option : options)
        {
            if (option.getCapability() != StorageCapability.LOOTING_BAG)
                continue;
            UimStorageDecision decision = evaluate(data, option);
            if (decision.isAllowed())
                return result(UimInventoryResolutionKind.USE_RESTRICTED_RETRIEVAL,
                        RecommendationConfidence.CHECK_NEEDED, decision, null,
                        PlayerText.get("UIRS8"));
        }

        for (UimStorageOption option : options)
        {
            StorageCapability capability = option.getCapability();
            if (!UimStorageMechanics.isDangerous(capability)
                    || !option.isMajorProgressionTransition()) continue;
            UimStorageDecision decision = evaluate(data, option);
            if (decision.isAllowed())
                return result(UimInventoryResolutionKind.USE_DANGEROUS_DEATH_STORAGE,
                        RecommendationConfidence.CHECK_NEEDED, decision,
                        RecommendationRiskDisclosure.deathStorage(),
                        PlayerText.get("UIRS9"));
        }
        return unresolved(PlayerText.get("UIRS10"));
    }

    private UimStorageDecision evaluate(StrategyDataBundle data,
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
                RecommendationConfidence.CHECK_NEEDED, null, null, reason);
    }

    private static UimInventoryResolution result(
            UimInventoryResolutionKind kind,
            RecommendationConfidence confidence,
            UimStorageDecision decision,
            RecommendationRiskDisclosure disclosure, String reason)
    {
        return new UimInventoryResolution(kind, confidence, decision,
                disclosure, reason);
    }
}
