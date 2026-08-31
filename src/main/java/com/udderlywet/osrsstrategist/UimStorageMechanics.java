package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

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
                return get(1189);
            case ZULRAH_ITEM_RETRIEVAL:
                return get(1190);
            case VOLCANIC_MINE_ITEM_RETRIEVAL:
                return get(1021);
            case DEATHPILE:
                return get(1191);
            case LOOTING_BAG:
                return "the looting bag";
            case POH_COSTUME_ROOM:
                return get(1192);
            case POH_STORAGE:
                return get(1193);
            case STASH:
                return get(1194);
            default:
                return capability.name().toLowerCase().replace('_', ' ');
        }
    }

    private static Map<StorageCapability, UimStorageMechanicProfile> profiles()
    {
        EnumMap<StorageCapability, UimStorageMechanicProfile> values =
                new EnumMap<>(StorageCapability.class);
        add(values, StorageCapability.LOOTING_BAG,
                get(1195),
                get(1032),
                get(1043),
                get(1051),
                get(1052),
                get(1053),
                get(1054),
                get(1055),
                RiskLevel.MEDIUM, StrategySourceId.UIM_ITEM_MANAGEMENT, true);
        add(values, StorageCapability.HESPORI_ITEM_RETRIEVAL,
                get(1056),
                get(1022),
                get(1023),
                get(1024),
                get(1025),
                "25,000 coins.",
                get(1026),
                get(1027),
                RiskLevel.HIGH, StrategySourceId.ITEM_RETRIEVAL_SERVICES, true);
        add(values, StorageCapability.ZULRAH_ITEM_RETRIEVAL,
                get(1196),
                get(1028),
                get(1029),
                get(1030),
                get(1031),
                get(1033),
                get(1034),
                get(1035),
                RiskLevel.HIGH, StrategySourceId.ITEM_RETRIEVAL_SERVICES, true);
        add(values, StorageCapability.VOLCANIC_MINE_ITEM_RETRIEVAL,
                get(1036),
                get(1037),
                get(1038),
                get(1039),
                get(1040),
                "150 numulite.",
                get(1041),
                get(1042),
                RiskLevel.HIGH, StrategySourceId.ITEM_RETRIEVAL_SERVICES, true);
        add(values, StorageCapability.DEATHPILE,
                get(1044),
                get(1045),
                get(1046),
                get(1047),
                get(1048),
                "No service fee.",
                get(1049),
                get(1050),
                RiskLevel.IRREVERSIBLE, StrategySourceId.UIM_ITEM_MANAGEMENT,
                true);
        values.put(StorageCapability.DEATH_STORAGE,
                new UimStorageMechanicProfile(StorageCapability.DEATH_STORAGE,
                        "Unknown", get(1197), "Unknown",
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
