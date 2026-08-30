package com.udderlywet.osrsstrategist;

/**
 * Storage systems that can materially change an account's viable strategy.
 *
 * <p>These are capabilities, not assumptions. A UIM should only be told to use
 * one of these when the matching {@link StorageSnapshot} entry is VERIFIED.</p>
 */
public enum StorageCapability
{
    TOOL_LEPRECHAUN,
    STASH,
    LOOTING_BAG,
    POH_COSTUME_ROOM,
    POH_STORAGE,
    /**
     * Legacy observation bucket retained for snapshot compatibility only.
     * It is not specific enough to authorize a storage recommendation.
     */
    DEATH_STORAGE,
    HESPORI_ITEM_RETRIEVAL,
    ZULRAH_ITEM_RETRIEVAL,
    VOLCANIC_MINE_ITEM_RETRIEVAL,
    DEATHPILE,
    SEED_BOX,
    HERB_SACK,
    RUNE_POUCH,
    GROUP_STORAGE
}
