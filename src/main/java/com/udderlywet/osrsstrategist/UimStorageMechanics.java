package com.udderlywet.osrsstrategist;

import java.util.*;

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
                return PlayerText.get("USM1");
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
                PlayerText.get("USM2"),
                PlayerText.get("USM3"),
                PlayerText.get("USM4"),
                PlayerText.get("USM5"),
                PlayerText.get("USM6"),
                PlayerText.get("USM7"),
                PlayerText.get("USM8"),
                RiskLevel.MEDIUM, StrategySourceId.UIM_ITEM_MANAGEMENT, true);
        add(values, StorageCapability.HESPORI_ITEM_RETRIEVAL,
                PlayerText.get("USM9"),
                PlayerText.get("USM10"),
                PlayerText.get("USM11"),
                PlayerText.get("USM12"),
                PlayerText.get("USM13"),
                "25,000 coins.",
                PlayerText.get("USM14"),
                PlayerText.get("USM15"),
                RiskLevel.HIGH, StrategySourceId.ITEM_RETRIEVAL_SERVICES, true);
        add(values, StorageCapability.ZULRAH_ITEM_RETRIEVAL,
                "Zul-Gwenwynig at the Zul-Andra dock",
                PlayerText.get("USM16"),
                PlayerText.get("USM17"),
                PlayerText.get("USM18"),
                PlayerText.get("USM19"),
                PlayerText.get("USM20"),
                PlayerText.get("USM21"),
                PlayerText.get("USM22"),
                RiskLevel.HIGH, StrategySourceId.ITEM_RETRIEVAL_SERVICES, true);
        add(values, StorageCapability.VOLCANIC_MINE_ITEM_RETRIEVAL,
                PlayerText.get("USM23"),
                PlayerText.get("USM24"),
                PlayerText.get("USM25"),
                PlayerText.get("USM26"),
                PlayerText.get("USM27"),
                "150 numulite.",
                PlayerText.get("USM28"),
                PlayerText.get("USM29"),
                RiskLevel.HIGH, StrategySourceId.ITEM_RETRIEVAL_SERVICES, true);
        add(values, StorageCapability.DEATHPILE,
                PlayerText.get("USM30"),
                PlayerText.get("USM31"),
                PlayerText.get("USM32"),
                PlayerText.get("USM33"),
                PlayerText.get("USM34"),
                "No service fee.",
                PlayerText.get("USM35"),
                PlayerText.get("USM36"),
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
