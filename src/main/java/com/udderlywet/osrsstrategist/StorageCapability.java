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
    DEATH_STORAGE,
    DEATHPILE,
    SEED_BOX,
    HERB_SACK,
    GROUP_STORAGE
}
