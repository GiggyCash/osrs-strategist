package com.udderlywet.osrsstrategist;

import java.util.EnumSet;
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
}
