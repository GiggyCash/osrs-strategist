package com.udderlywet.osrsstrategist;

import java.util.EnumSet;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Verified behavior classes for UIM storage; distinct systems never alias. */
public final class UimStorageMechanics
{
    private static final Set<StorageCapability> ITEM_RETRIEVAL_SERVICES =
            EnumSet.of(StorageCapability.HESPORI_ITEM_RETRIEVAL,
                    StorageCapability.ZULRAH_ITEM_RETRIEVAL,
                    StorageCapability.VOLCANIC_MINE_ITEM_RETRIEVAL);
    private static final Set<StorageCapability> RESTRICTED_RETRIEVAL =
            EnumSet.of(StorageCapability.LOOTING_BAG,
                    StorageCapability.DEATH_STORAGE,
                    StorageCapability.HESPORI_ITEM_RETRIEVAL,
                    StorageCapability.ZULRAH_ITEM_RETRIEVAL,
                    StorageCapability.VOLCANIC_MINE_ITEM_RETRIEVAL,
                    StorageCapability.DEATHPILE);
    private static final Map<StorageCapability, UimStorageMechanicProfile>
            PROFILES = profiles();

    private UimStorageMechanics() {}

    public static boolean isExactItemRetrievalService(
            StorageCapability capability)
    {
        return capability != null
                && ITEM_RETRIEVAL_SERVICES.contains(capability);
    }

    public static boolean isDangerous(StorageCapability capability)
    {
        return isExactItemRetrievalService(capability)
                || capability == StorageCapability.DEATHPILE;
    }

    public static boolean isRestrictedRetrieval(StorageCapability capability)
    {
        return capability != null && RESTRICTED_RETRIEVAL.contains(capability);
    }

    /** Generic death storage lacks the location-specific rules needed to act. */
    public static boolean isTooGenericToRecommend(StorageCapability capability)
    {
        return capability == StorageCapability.DEATH_STORAGE;
    }

    public static UimStorageMechanicProfile profile(
            StorageCapability capability)
    {
        return capability == null ? null : PROFILES.get(capability);
    }

    public static String displayName(StorageCapability capability)
    {
        if (capability == null) return "unknown storage";
        switch (capability)
        {
            case HESPORI_ITEM_RETRIEVAL:
                return "Arno's Hespori Item Retrieval Service";
            case ZULRAH_ITEM_RETRIEVAL:
                return "the Zulrah Item Retrieval Service";
            case VOLCANIC_MINE_ITEM_RETRIEVAL:
                return "Petrified Pete's Volcanic Mine Item Retrieval Service";
            case DEATHPILE:
                return "an on-ground UIM deathpile";
            case LOOTING_BAG:
                return "the looting bag";
            case POH_COSTUME_ROOM:
                return "the verified POH costume-room storage";
            case POH_STORAGE:
                return "the verified POH storage";
            case STASH:
                return "the verified STASH unit";
            default:
                return capability.name().toLowerCase().replace('_', ' ');
        }
    }

    private static Map<StorageCapability, UimStorageMechanicProfile> profiles()
    {
        EnumMap<StorageCapability, UimStorageMechanicProfile> values =
                new EnumMap<>(StorageCapability.class);
        add(values, StorageCapability.LOOTING_BAG,
                "Ferox Enclave or the Wilderness",
                "A looting bag must be owned; insertion access and the intended retrieval route must be observed.",
                "Up to 28 tradeable item stacks; untradeable items are excluded.",
                "Items may be inserted in the Wilderness or Ferox Enclave. Ferox is safe for insertion but is not Wilderness for destruction.",
                "Contents normally require death retrieval, or destruction and immediate pickup in the Wilderness. Destroying the bag outside the Wilderness, including Ferox Enclave, permanently loses the contents.",
                "No retrieval fee, but rebuilding the bag and route setup have real cost.",
                "The bag itself has no storage timer; any resulting ground pile uses its own death/drop timer.",
                "Death-based retrieval inherits the exact destination service or deathpile rules; another unsafe death can destroy service-held items.",
                RiskLevel.MEDIUM, StrategySourceId.UIM_ITEM_MANAGEMENT, true);
        add(values, StorageCapability.HESPORI_ITEM_RETRIEVAL,
                "Arno outside the Hespori cave in the Farming Guild",
                "65 Farming is boostable and a fully grown Hespori must be available; Farming Guild and cave access must be verified.",
                "Only items accepted by the Hespori death service for the exact death are eligible.",
                "Items enter Arno's service only through the verified Hespori death mechanic; it is not a normal deposit container.",
                "Retrieve the service contents from Arno. The 25,000-coin fee may be taken from the stored contents.",
                "25,000 coins.",
                "No ordinary expiry is modeled; the second-death rule remains active until everything is reclaimed.",
                "Any unsafe death before full retrieval permanently destroys every item still held by the service.",
                RiskLevel.HIGH, StrategySourceId.ITEM_RETRIEVAL_SERVICES, true);
        add(values, StorageCapability.ZULRAH_ITEM_RETRIEVAL,
                "Zul-Gwenwynig at the Zul-Andra dock",
                "Zul-Andra and Zulrah access must be verified before the death mechanic can be used.",
                "Only items accepted by the Zulrah death service for the exact death are eligible.",
                "Items enter the priestess's service only through a death at Zulrah; it is not a normal deposit container.",
                "Ultimate Ironmen reclaim directly from Zul-Gwenwynig rather than the ordinary chest interface.",
                "Free for Ultimate Ironmen regardless of Zulrah kill count.",
                "No ordinary expiry is modeled; the second-death rule remains active until everything is reclaimed.",
                "Any unsafe death before full retrieval permanently destroys every item still held by the service.",
                RiskLevel.HIGH, StrategySourceId.ITEM_RETRIEVAL_SERVICES, true);
        add(values, StorageCapability.VOLCANIC_MINE_ITEM_RETRIEVAL,
                "Petrified Pete at the Volcanic Mine entrance on Fossil Island",
                "Volcanic Mine access and enough numulite for entry/retrieval must be verified.",
                "Only items accepted after a death in Volcanic Mine are eligible.",
                "Items enter Pete's service only through the Volcanic Mine death mechanic; it is not a normal deposit container.",
                "Retrieve the held items from Petrified Pete; stored numulite may pay the retrieval fee.",
                "150 numulite.",
                "No ordinary expiry is modeled; the second-death rule remains active until everything is reclaimed.",
                "Any unsafe death before full retrieval permanently destroys every item still held by the service.",
                RiskLevel.HIGH, StrategySourceId.ITEM_RETRIEVAL_SERVICES, true);
        add(values, StorageCapability.DEATHPILE,
                "The exact tile of a verified ordinary non-instanced UIM death",
                "The death tile, return route, login-time window, item count, PvP interaction state, and all item-specific death behavior must be verified.",
                "Only items proven to drop under the exact death context are eligible; Wilderness food/potions and special items need separate rules.",
                "Items are placed by an ordinary UIM death, never by a generic storage command.",
                "Return to the exact tile and recover every retained item before its timer expires.",
                "No service fee.",
                "60 minutes of logged-in time; logging out pauses the timer.",
                "A later ordinary death does not refresh the earlier pile. PvP kill-credit interactions can change Wilderness safety, so unknown state fails closed.",
                RiskLevel.IRREVERSIBLE, StrategySourceId.UIM_ITEM_MANAGEMENT,
                true);
        values.put(StorageCapability.DEATH_STORAGE,
                new UimStorageMechanicProfile(StorageCapability.DEATH_STORAGE,
                        "Unknown", "Unknown exact service", "Unknown",
                        "Unknown", "Unknown", "Unknown", "Unknown",
                        "Unknown", RiskLevel.HIGH,
                        StrategySourceId.ITEM_RETRIEVAL_SERVICES, false));
        return Collections.unmodifiableMap(values);
    }

    private static void add(
            Map<StorageCapability, UimStorageMechanicProfile> values,
            StorageCapability capability, String location,
            String accessRequirements, String eligibleItems,
            String insertionRules, String retrievalRules, String cost,
            String expiration, String secondDeathBehavior, RiskLevel risk,
            StrategySourceId source, boolean recommendationEligible)
    {
        values.put(capability, new UimStorageMechanicProfile(capability,
                location, accessRequirements, eligibleItems, insertionRules,
                retrievalRules, cost, expiration, secondDeathBehavior, risk,
                source, recommendationEligible));
    }
}
