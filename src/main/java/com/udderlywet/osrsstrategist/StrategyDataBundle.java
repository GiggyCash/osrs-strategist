package com.udderlywet.osrsstrategist;

/**
 * Immutable bundle containing everything the strategy engine knows about the
 * currently logged-in character.
 *
 * <p>Most fields are intentionally nullable. In Strategist, {@code null}
 * means "this source has not been observed yet", not "the player has none".
 * That distinction is the foundation of the project's no-guessing rule.</p>
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
    private final AccessMemorySnapshot accessMemory;

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

    /** Backwards-compatible constructor used by early skeleton tests. */
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
        this(
                account, inventory, bank, equipment, quests, diaries, clue,
                combatAchievements, collectionLog, economy, capabilities,
                null, null, null, null, null, null, null, null, null, null, null
        );
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

    public static Builder builder(AccountSnapshot account)
    {
        return new Builder(account);
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
    public AccessMemorySnapshot getAccessMemory() { return accessMemory; }
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

        public Builder inventory(InventorySnapshot value) { inventory = value; return this; }
        public Builder bank(BankSnapshot value) { bank = value; return this; }
        public Builder equipment(EquipmentSnapshot value) { equipment = value; return this; }
        public Builder quests(QuestSnapshot value) { quests = value; return this; }
        public Builder diaries(DiarySnapshot value) { diaries = value; return this; }
        public Builder clue(ClueSnapshot value) { clue = value; return this; }
        public Builder combatAchievements(CombatAchievementSnapshot value) { combatAchievements = value; return this; }
        public Builder collectionLog(CollectionLogSnapshot value) { collectionLog = value; return this; }
        public Builder economy(AccountEconomySnapshot value) { economy = value; return this; }
        public Builder capabilities(AccountCapabilities value) { capabilities = value; return this; }
        public Builder accessMemory(AccessMemorySnapshot value) { accessMemory = value; return this; }
        public Builder storage(StorageSnapshot value) { storage = value; return this; }
        public Builder transport(TransportSnapshot value) { transport = value; return this; }
        public Builder poh(PohSnapshot value) { poh = value; return this; }
        public Builder groupStorage(GroupStorageSnapshot value) { groupStorage = value; return this; }
        public Builder slayer(SlayerSnapshot value) { slayer = value; return this; }
        public Builder farming(FarmingSnapshot value) { farming = value; return this; }
        public Builder sailing(SailingSnapshot value) { sailing = value; return this; }
        public Builder minigames(MinigameSnapshot value) { minigames = value; return this; }
        public Builder pvm(PvmSnapshot value) { pvm = value; return this; }
        public Builder recurringOpportunities(RecurringOpportunitySnapshot value) { recurringOpportunities = value; return this; }

        public StrategyDataBundle build()
        {
            return new StrategyDataBundle(
                    account, inventory, bank, equipment, quests, diaries, clue,
                    combatAchievements, collectionLog, economy, capabilities,
                    accessMemory, storage, transport, poh, groupStorage, slayer,
                    farming, sailing, minigames, pvm, recurringOpportunities
            );
        }
    }
}
