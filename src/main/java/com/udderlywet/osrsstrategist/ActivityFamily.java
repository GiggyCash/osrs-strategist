package com.udderlywet.osrsstrategist;

/**
 * Broad activity families used only for gentle variety/engagement scoring.
 *
 * <p>These are intentionally broader than individual activities. Exact player
 * preference remains attached to stable activity IDs in {@link PreferenceProfile};
 * this family layer only notices repeated patterns such as repeatedly avoiding
 * combat or spending several completed recommendations in a row on production
 * skills.</p>
 */
public enum ActivityFamily
{
    COMBAT,
    GATHERING,
    PRODUCTION,
    UTILITY,
    FARMING,
    SAILING,
    QUEST,
    PVM,
    CLUE,
    DIARY,
    MINIGAME,
    GEAR,
    MONEY,
    COLLECTION,
    OTHER
}
