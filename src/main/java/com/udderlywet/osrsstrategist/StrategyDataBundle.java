package com.udderlywet.osrsstrategist;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Immutable bundle containing everything Compass currently knows. */
@Getter
@Builder(builderClassName = "Builder", builderMethodName = "newBuilder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class StrategyDataBundle
{
    private final AccountSnapshot account;
    private final InventorySnapshot inventory;
    private final BankSnapshot bank;
    private final EquipmentSnapshot equipment;
    private final QuestSnapshot quests;
    private final DiarySnapshot diaries;
    private final ClueSnapshot clue;
    private final CombatAchievementSnapshot combatAchievements;
    private final CollectionLogSnapshot collectionLog;
    private final AccountEconomySnapshot economy;
    private final AccountCapabilities capabilities;
    private final AccessMemorySnapshot accessMemory;
    private final FarmingRunSnapshot farmingRuns;
    private final StorageSnapshot storage;
    private final TransportSnapshot transport;
    private final PohSnapshot poh;
    private final GroupStorageSnapshot groupStorage;
    private final SlayerSnapshot slayer;
    private final FarmingSnapshot farming;
    private final SailingSnapshot sailing;
    private final MinigameSnapshot minigames;
    private final PvmSnapshot pvm;
    private final RecurringOpportunitySnapshot recurringOpportunities;
    private final CombatEvidenceSnapshot combatEvidence;

    public StrategyDataBundle(
            AccountSnapshot account,
            InventorySnapshot inventory,
            BankSnapshot bank,
            EquipmentSnapshot equipment,
            QuestSnapshot quests,
            DiarySnapshot diaries,
            ClueSnapshot clue,
            CombatAchievementSnapshot combatAchievements,
            CollectionLogSnapshot collectionLog,
            AccountEconomySnapshot economy,
            AccountCapabilities capabilities)
    {
        this(account, inventory, bank, equipment, quests, diaries, clue,
                combatAchievements, collectionLog, economy, capabilities,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null);
    }

    public static Builder builder(AccountSnapshot account)
    {
        return newBuilder().account(account);
    }
}
