package com.udderlywet.osrsstrategist;

/** Immutable bundle containing everything Strategist currently knows. */
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
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private StrategyDataBundle(
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
            AccountCapabilities capabilities,
            AccessMemorySnapshot accessMemory,
            FarmingRunSnapshot farmingRuns,
            StorageSnapshot storage,
            TransportSnapshot transport,
            PohSnapshot poh,
            GroupStorageSnapshot groupStorage,
            SlayerSnapshot slayer,
            FarmingSnapshot farming,
            SailingSnapshot sailing,
            MinigameSnapshot minigames,
            PvmSnapshot pvm,
            RecurringOpportunitySnapshot recurringOpportunities)
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
        this.accessMemory = accessMemory;
        this.farmingRuns = farmingRuns;
        this.storage = storage;
        this.transport = transport;
        this.poh = poh;
        this.groupStorage = groupStorage;
        this.slayer = slayer;
        this.farming = farming;
        this.sailing = sailing;
        this.minigames = minigames;
        this.pvm = pvm;
        this.recurringOpportunities = recurringOpportunities;
    }

    public static Builder builder(AccountSnapshot account) { return new Builder(account); }

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
    public AccessMemorySnapshot getAccessMemory() { return accessMemory; }
    public FarmingRunSnapshot getFarmingRuns() { return farmingRuns; }
    public StorageSnapshot getStorage() { return storage; }
    public TransportSnapshot getTransport() { return transport; }
    public PohSnapshot getPoh() { return poh; }
    public GroupStorageSnapshot getGroupStorage() { return groupStorage; }
    public SlayerSnapshot getSlayer() { return slayer; }
    public FarmingSnapshot getFarming() { return farming; }
    public SailingSnapshot getSailing() { return sailing; }
    public MinigameSnapshot getMinigames() { return minigames; }
    public PvmSnapshot getPvm() { return pvm; }
    public RecurringOpportunitySnapshot getRecurringOpportunities() { return recurringOpportunities; }

    public static final class Builder
    {
        private final AccountSnapshot account;
        private InventorySnapshot inventory;
        private BankSnapshot bank;
        private EquipmentSnapshot equipment;
        private QuestSnapshot quests;
        private DiarySnapshot diaries;
        private ClueSnapshot clue;
        private CombatAchievementSnapshot combatAchievements;
        private CollectionLogSnapshot collectionLog;
        private AccountEconomySnapshot economy;
        private AccountCapabilities capabilities;
        private AccessMemorySnapshot accessMemory;
        private FarmingRunSnapshot farmingRuns;
        private StorageSnapshot storage;
        private TransportSnapshot transport;
        private PohSnapshot poh;
        private GroupStorageSnapshot groupStorage;
        private SlayerSnapshot slayer;
        private FarmingSnapshot farming;
        private SailingSnapshot sailing;
        private MinigameSnapshot minigames;
        private PvmSnapshot pvm;
        private RecurringOpportunitySnapshot recurringOpportunities;

        private Builder(AccountSnapshot account) { this.account = account; }
        public Builder inventory(InventorySnapshot v) { inventory = v; return this; }
        public Builder bank(BankSnapshot v) { bank = v; return this; }
        public Builder equipment(EquipmentSnapshot v) { equipment = v; return this; }
        public Builder quests(QuestSnapshot v) { quests = v; return this; }
        public Builder diaries(DiarySnapshot v) { diaries = v; return this; }
        public Builder clue(ClueSnapshot v) { clue = v; return this; }
        public Builder combatAchievements(CombatAchievementSnapshot v) { combatAchievements = v; return this; }
        public Builder collectionLog(CollectionLogSnapshot v) { collectionLog = v; return this; }
        public Builder economy(AccountEconomySnapshot v) { economy = v; return this; }
        public Builder capabilities(AccountCapabilities v) { capabilities = v; return this; }
        public Builder accessMemory(AccessMemorySnapshot v) { accessMemory = v; return this; }
        public Builder farmingRuns(FarmingRunSnapshot v) { farmingRuns = v; return this; }
        public Builder storage(StorageSnapshot v) { storage = v; return this; }
        public Builder transport(TransportSnapshot v) { transport = v; return this; }
        public Builder poh(PohSnapshot v) { poh = v; return this; }
        public Builder groupStorage(GroupStorageSnapshot v) { groupStorage = v; return this; }
        public Builder slayer(SlayerSnapshot v) { slayer = v; return this; }
        public Builder farming(FarmingSnapshot v) { farming = v; return this; }
        public Builder sailing(SailingSnapshot v) { sailing = v; return this; }
        public Builder minigames(MinigameSnapshot v) { minigames = v; return this; }
        public Builder pvm(PvmSnapshot v) { pvm = v; return this; }
        public Builder recurringOpportunities(RecurringOpportunitySnapshot v) { recurringOpportunities = v; return this; }

        public StrategyDataBundle build()
        {
            return new StrategyDataBundle(
                    account, inventory, bank, equipment, quests, diaries, clue,
                    combatAchievements, collectionLog, economy, capabilities,
                    accessMemory, farmingRuns, storage, transport, poh,
                    groupStorage, slayer, farming, sailing, minigames, pvm,
                    recurringOpportunities);
        }
    }
}
