package com.udderlywet.osrsstrategist;

/**
 * High-level source of a strategy signal.
 *
 * <p>Signals let specialized systems contribute to one strategy decision
 * without coupling the skill engine to quests, PvM, clues, the economy, or
 * future game systems.</p>
 */
public enum StrategySignalCategory
{
    GOAL,
    SKILL,
    QUEST,
    DIARY,
    OPPORTUNITY,
    RESOURCE,
    ECONOMY,
    GEAR,
    PVM,
    CLUE,
    COMBAT_ACHIEVEMENT,
    COLLECTION_LOG,
    MINIGAME,
    TRANSPORT,
    POH,
    STORAGE,
    SAILING,
    SLAYER,
    FARMING,
    ACCOUNT_MODE
}
