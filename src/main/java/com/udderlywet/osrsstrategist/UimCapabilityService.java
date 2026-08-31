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
                    PlayerText.get("UCS1"));
        }

        if (UimStorageMechanics.isTooGenericToRecommend(capability))
        {
            return decision(capability, false,
                    RecommendationConfidence.CHECK_NEEDED,
                    RiskLevel.HIGH,
                    PlayerText.get("UCS2"));
        }

        if (UimStorageMechanics.isRestrictedRetrieval(capability))
        {
            UimStorageMechanicProfile mechanic =
                    UimStorageMechanics.profile(capability);
            if (mechanic == null
                    || !mechanic.hasCompleteRecommendationRules())
                return decision(capability, false,
                        RecommendationConfidence.CHECK_NEEDED,
                        riskFor(capability),
                        PlayerText.get("UCS3"));
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
                    PlayerText.get("UCS4"));
        }
        if (capabilityState != CapabilityState.VERIFIED)
        {
            return decision(capability, false,
                    RecommendationConfidence.CHECK_NEEDED,
                    riskFor(capability),
                    PlayerText.get("UCS5"));
        }

        if (itemCompatibility == CapabilityState.BLOCKED)
        {
            return decision(capability, false,
                    RecommendationConfidence.BLOCKED,
                    riskFor(capability),
                    PlayerText.get("UCS6"));
        }
        if (itemCompatibility != CapabilityState.VERIFIED)
        {
            return decision(capability, false,
                    RecommendationConfidence.CHECK_NEEDED,
                    riskFor(capability),
                    PlayerText.get("UCS7"));
        }

        if (capacityOrPreconditions == CapabilityState.BLOCKED)
        {
            return decision(capability, false,
                    RecommendationConfidence.BLOCKED,
                    riskFor(capability),
                    PlayerText.get("UCS8"));
        }
        if (capacityOrPreconditions != CapabilityState.VERIFIED)
        {
            return decision(capability, false,
                    RecommendationConfidence.CHECK_NEEDED,
                    riskFor(capability),
                    PlayerText.get("UCS9"));
        }

        return decision(capability, true,
                RecommendationConfidence.VERIFIED,
                riskFor(capability),
                PlayerText.get("UCS10"));
    }

    public boolean shouldRequireExplicitWarning(StorageCapability capability)
    {
        RiskLevel risk = riskFor(capability);
        return risk == RiskLevel.HIGH || risk == RiskLevel.IRREVERSIBLE;
    }

    private static RiskLevel riskFor(StorageCapability capability)
    {
        UimStorageMechanicProfile profile =
                UimStorageMechanics.profile(capability);
        if (profile != null) return profile.getRisk();
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
