package com.udderlywet.osrsstrategist;

/**
 * One place for every account-facing data source the strategy engine will
 * eventually reason over. Fields may be null until RuneLite can verify them;
 * unknown data must never be silently guessed.
 */
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
        this.account = account;
        this.inventory = inventory;
        this.bank = bank;
        this.equipment = equipment;
        this.quests = quests;
        this.diaries = diaries;
        this.clue = clue;
        this.combatAchievements = combatAchievements;
        this.collectionLog = collectionLog;
        this.economy = economy;
        this.capabilities = capabilities;
    }

    public AccountSnapshot getAccount() { return account; }
    public InventorySnapshot getInventory() { return inventory; }
    public BankSnapshot getBank() { return bank; }
    public EquipmentSnapshot getEquipment() { return equipment; }
    public QuestSnapshot getQuests() { return quests; }
    public DiarySnapshot getDiaries() { return diaries; }
    public ClueSnapshot getClue() { return clue; }
    public CombatAchievementSnapshot getCombatAchievements() { return combatAchievements; }
    public CollectionLogSnapshot getCollectionLog() { return collectionLog; }
    public AccountEconomySnapshot getEconomy() { return economy; }
    public AccountCapabilities getCapabilities() { return capabilities; }
}
