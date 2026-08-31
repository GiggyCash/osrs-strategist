package compass;
import static compass.Text.get;

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
            GameData data,
            StorageCapability capability,
            CapabilityState itemCompatibility,
            CapabilityState capacityOrPreconditions)
    {
        if (data == null || data.account() == null)
        {
            return decision(capability, false,
                    Confidence.CHECK_NEEDED,
                    riskFor(capability),
                    get(1421));
        }

        AccountMode mode = AccountMode.fromTypeCode(
                data.account().modeCode()
        );
        if (mode != AccountMode.ULTIMATE_IRONMAN)
        {
            return decision(capability, false,
                    Confidence.CHECK_NEEDED,
                    RiskLevel.NONE,
                    get(940));
        }

        if (UimStorageMechanics.isTooGenericToRecommend(capability))
        {
            return decision(capability, false,
                    Confidence.CHECK_NEEDED,
                    RiskLevel.HIGH,
                    get(942));
        }

        if (UimStorageMechanics.isRestrictedRetrieval(capability))
        {
            UimStorageMechanicProfile mechanic =
                    UimStorageMechanics.profile(capability);
            if (mechanic == null
                    || !mechanic.hasCompleteRecommendationRules())
                return decision(capability, false,
                        Confidence.CHECK_NEEDED,
                        riskFor(capability),
                        get(943));
        }

        var storage = data.storage();
        CapabilityState capabilityState = storage == null
                ? CapabilityState.UNKNOWN
                : storage.stateOf(capability);

        if (capabilityState == CapabilityState.BLOCKED)
        {
            return decision(capability, false,
                    Confidence.BLOCKED,
                    riskFor(capability),
                    get(944));
        }
        if (capabilityState != CapabilityState.VERIFIED)
        {
            return decision(capability, false,
                    Confidence.CHECK_NEEDED,
                    riskFor(capability),
                    get(945));
        }

        if (itemCompatibility == CapabilityState.BLOCKED)
        {
            return decision(capability, false,
                    Confidence.BLOCKED,
                    riskFor(capability),
                    get(946));
        }
        if (itemCompatibility != CapabilityState.VERIFIED)
        {
            return decision(capability, false,
                    Confidence.CHECK_NEEDED,
                    riskFor(capability),
                    get(947));
        }

        if (capacityOrPreconditions == CapabilityState.BLOCKED)
        {
            return decision(capability, false,
                    Confidence.BLOCKED,
                    riskFor(capability),
                    get(948));
        }
        if (capacityOrPreconditions != CapabilityState.VERIFIED)
        {
            return decision(capability, false,
                    Confidence.CHECK_NEEDED,
                    riskFor(capability),
                    get(949));
        }

        return decision(capability, true,
                Confidence.VERIFIED,
                riskFor(capability),
                get(941));
    }

    public boolean shouldRequireExplicitWarning(StorageCapability capability)
    {
        var risk = riskFor(capability);
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
            Confidence confidence,
            RiskLevel risk,
            String explanation)
    {
        return new UimStorageDecision(
                capability, allowed, confidence, risk, explanation
        );
    }
}
