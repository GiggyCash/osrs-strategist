package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/**
 * Conservative capability gate for Ultimate Ironman storage suggestions.
 *
 * <p>A storage system being possible in OSRS is never enough. Compass needs
 * direct evidence that this character has the capability and, for an item-
 * specific route, evidence that the item is compatible and that required space
 * or preconditions are satisfied.</p>
 */
@Singleton
public class UimCapabilityService
{
    public UimStorageDecision evaluateStorage(
            StrategyDataBundle data,
            StorageCapability capability,
            CapabilityState itemCompatibility,
            CapabilityState capacityOrPreconditions)
    {
        if (data == null || data.getAccount() == null)
        {
            return decision(capability, false,
                    RecommendationConfidence.CHECK_NEEDED,
                    riskFor(capability),
                    "Account state has not been observed.");
        }

        AccountMode mode = AccountMode.fromTypeCode(
                data.getAccount().getAccountTypeCode()
        );
        if (mode != AccountMode.ULTIMATE_IRONMAN)
        {
            return decision(capability, false,
                    RecommendationConfidence.CHECK_NEEDED,
                    RiskLevel.NONE,
                    "UIM capability routing only applies to Ultimate Ironman accounts.");
        }

        if (UimStorageMechanics.isTooGenericToRecommend(capability))
        {
            return decision(capability, false,
                    RecommendationConfidence.CHECK_NEEDED,
                    RiskLevel.HIGH,
                    "Generic death-storage evidence does not identify the retrieval service, access, withdrawal order, or second-death rules. Verify an exact service first.");
        }

        StorageSnapshot storage = data.getStorage();
        CapabilityState capabilityState = storage == null
                ? CapabilityState.UNKNOWN
                : storage.stateOf(capability);

        if (capabilityState == CapabilityState.BLOCKED)
        {
            return decision(capability, false,
                    RecommendationConfidence.BLOCKED,
                    riskFor(capability),
                    "This storage capability has been observed as unavailable.");
        }
        if (capabilityState != CapabilityState.VERIFIED)
        {
            return decision(capability, false,
                    RecommendationConfidence.CHECK_NEEDED,
                    riskFor(capability),
                    "This character's access to the storage system has not been verified.");
        }

        if (itemCompatibility == CapabilityState.BLOCKED)
        {
            return decision(capability, false,
                    RecommendationConfidence.BLOCKED,
                    riskFor(capability),
                    "The intended item has been verified as incompatible with this storage route.");
        }
        if (itemCompatibility != CapabilityState.VERIFIED)
        {
            return decision(capability, false,
                    RecommendationConfidence.CHECK_NEEDED,
                    riskFor(capability),
                    "Item compatibility with this storage route has not been verified.");
        }

        if (capacityOrPreconditions == CapabilityState.BLOCKED)
        {
            return decision(capability, false,
                    RecommendationConfidence.BLOCKED,
                    riskFor(capability),
                    "Required capacity or preconditions are not currently satisfied.");
        }
        if (capacityOrPreconditions != CapabilityState.VERIFIED)
        {
            return decision(capability, false,
                    RecommendationConfidence.CHECK_NEEDED,
                    riskFor(capability),
                    "Required capacity or preconditions have not been verified.");
        }

        return decision(capability, true,
                RecommendationConfidence.VERIFIED,
                riskFor(capability),
                "Capability, item compatibility, and current preconditions are verified.");
    }

    public boolean shouldRequireExplicitWarning(StorageCapability capability)
    {
        RiskLevel risk = riskFor(capability);
        return risk == RiskLevel.HIGH || risk == RiskLevel.IRREVERSIBLE;
    }

    private static RiskLevel riskFor(StorageCapability capability)
    {
        if (capability == StorageCapability.DEATH_STORAGE
                || UimStorageMechanics.isExactItemRetrievalService(capability))
        {
            return RiskLevel.HIGH;
        }
        if (capability == StorageCapability.DEATHPILE)
        {
            return RiskLevel.IRREVERSIBLE;
        }
        if (capability == StorageCapability.LOOTING_BAG)
        {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private static UimStorageDecision decision(
            StorageCapability capability,
            boolean allowed,
            RecommendationConfidence confidence,
            RiskLevel risk,
            String explanation)
    {
        return new UimStorageDecision(
                capability, allowed, confidence, risk, explanation
        );
    }
}
