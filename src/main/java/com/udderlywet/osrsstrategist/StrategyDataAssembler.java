package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Builds one immutable StrategyDataBundle from live and remembered evidence. */
@Singleton
public class StrategyDataAssembler
{
    private final AccountReader accountReader;
    private final LiveItemStateReader itemStateReader;
    private final LiveRunePouchStateReader runePouchStateReader;
    private final LiveQuestStateReader questStateReader;
    private final LiveDiaryStateReader diaryStateReader;
    private final LiveCombatAchievementReader combatAchievementReader;
    private final LiveClueStateReader clueStateReader;
    private final LiveSlayerStateReader slayerStateReader;
    private final LiveEconomyReader economyReader;
    private final PvmReadinessAnalyzer pvmReadinessAnalyzer;
    private final AccountAccessMemoryStore accessMemoryStore;
    private final FarmingRunStateStore farmingRunStateStore;
    private final FarmingAccessEvaluator farmingAccessEvaluator;
    private final ObservedStateStore observedStateStore;

    @Inject
    public StrategyDataAssembler(
            AccountReader accountReader,
            LiveItemStateReader itemStateReader,
            LiveRunePouchStateReader runePouchStateReader,
            LiveQuestStateReader questStateReader,
            LiveDiaryStateReader diaryStateReader,
            LiveCombatAchievementReader combatAchievementReader,
            LiveClueStateReader clueStateReader,
            LiveSlayerStateReader slayerStateReader,
            LiveEconomyReader economyReader,
            PvmReadinessAnalyzer pvmReadinessAnalyzer,
            AccountAccessMemoryStore accessMemoryStore,
            FarmingRunStateStore farmingRunStateStore,
            FarmingAccessEvaluator farmingAccessEvaluator,
            ObservedStateStore observedStateStore)
    {
        this.accountReader = accountReader;
        this.itemStateReader = itemStateReader;
        this.runePouchStateReader = runePouchStateReader;
        this.questStateReader = questStateReader;
        this.diaryStateReader = diaryStateReader;
        this.combatAchievementReader = combatAchievementReader;
        this.clueStateReader = clueStateReader;
        this.slayerStateReader = slayerStateReader;
        this.economyReader = economyReader;
        this.pvmReadinessAnalyzer = pvmReadinessAnalyzer;
        this.accessMemoryStore = accessMemoryStore;
        this.farmingRunStateStore = farmingRunStateStore;
        this.farmingAccessEvaluator = farmingAccessEvaluator;
        this.observedStateStore = observedStateStore;
    }

    /** Compatibility constructor for focused tests created before live readers/analyzers. */
    public StrategyDataAssembler(
            AccountReader accountReader,
            LiveItemStateReader itemStateReader,
            LiveQuestStateReader questStateReader,
            AccountAccessMemoryStore accessMemoryStore,
            FarmingRunStateStore farmingRunStateStore,
            FarmingAccessEvaluator farmingAccessEvaluator,
            ObservedStateStore observedStateStore)
    {
        this(accountReader, itemStateReader, null, questStateReader,
                null, null, null, null, null, null,
                accessMemoryStore, farmingRunStateStore,
                farmingAccessEvaluator, observedStateStore);
    }

    public StrategyDataBundle read()
    {
        AccountSnapshot account = accountReader.read();
        if (account == null) return null;

        InventorySnapshot inventory = itemStateReader.readInventory();
        BankSnapshot bank = itemStateReader.readBank();
        EquipmentSnapshot equipment = itemStateReader.readEquipment();
        StorageSnapshot rememberedStorage = observedStateStore.getStorage();
        StorageSnapshot storage = runePouchStateReader == null
                ? rememberedStorage
                : runePouchStateReader.merge(rememberedStorage, inventory);

        AccountEconomySnapshot liveEconomy = economyReader == null
                ? null : economyReader.read(account, inventory, bank);
        AccountEconomySnapshot economy = liveEconomy != null
                ? liveEconomy : observedStateStore.getEconomy();

        QuestSnapshot liveQuests = questStateReader.read();
        QuestSnapshot quests = liveQuests != null
                ? liveQuests : observedStateStore.getQuests();
        DiarySnapshot liveDiaries = diaryStateReader == null
                ? null : diaryStateReader.read();
        DiarySnapshot diaries = liveDiaries != null
                ? liveDiaries : observedStateStore.getDiaries();
        CombatAchievementSnapshot observedCombatAchievements =
                observedStateStore.getCombatAchievements();
        CombatAchievementSnapshot combatAchievements =
                combatAchievementReader == null
                        ? observedCombatAchievements
                        : combatAchievementReader.read(observedCombatAchievements);

        AccountMode accountMode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        ClueSnapshot rememberedClue = observedStateStore.getClue();
        ClueSnapshot clue = clueStateReader == null
                ? rememberedClue
                : clueStateReader.read(accountMode, inventory, bank, rememberedClue);
        if (clueStateReader != null && clue != rememberedClue)
        {
            observedStateStore.setClue(clue);
        }

        SlayerSnapshot liveSlayer = slayerStateReader == null
                ? null : slayerStateReader.read();
        SlayerSnapshot slayer = liveSlayer != null
                ? liveSlayer : observedStateStore.getSlayer();

        PvmSnapshot observedPvm = observedStateStore.getPvm();
        PvmSnapshot pvm = pvmReadinessAnalyzer == null
                ? observedPvm
                : pvmReadinessAnalyzer.analyze(
                        account, quests, equipment, inventory, bank, observedPvm);
        AccessMemorySnapshot accessMemory = accessMemoryStore.snapshot();
        FarmingSnapshot farming = farmingAccessEvaluator.evaluate(
                account, quests, accessMemory, observedStateStore.getFarming());

        return StrategyDataBundle.builder(account)
                .inventory(inventory)
                .bank(bank)
                .equipment(equipment)
                .quests(quests)
                .diaries(diaries)
                .clue(clue)
                .combatAchievements(combatAchievements)
                .collectionLog(observedStateStore.getCollectionLog())
                .economy(economy)
                .capabilities(observedStateStore.getCapabilities())
                .accessMemory(accessMemory)
                .farmingRuns(farmingRunStateStore.snapshot())
                .storage(storage)
                .transport(observedStateStore.getTransport())
                .poh(observedStateStore.getPoh())
                .groupStorage(observedStateStore.getGroupStorage())
                .slayer(slayer)
                .farming(farming)
                .sailing(observedStateStore.getSailing())
                .minigames(observedStateStore.getMinigames())
                .pvm(pvm)
                .recurringOpportunities(observedStateStore.getRecurringOpportunities())
                .build();
    }

    public void clearForAccountChange()
    {
        itemStateReader.clearAccountCaches();
        if (slayerStateReader != null) slayerStateReader.clear();
        observedStateStore.clearForAccountChange();
    }
}
