package compass;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Builds one immutable GameData from live and remembered evidence. */
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
    private final LivePohStateReader pohStateReader;
    private final LiveSailingStateReader sailingStateReader;
    private final LiveEconomyReader economyReader;
    private final LiveCombatEvidenceReader combatEvidenceReader;
    private final PvmReadinessAnalyzer pvmReadinessAnalyzer;
    private final AccountAccessMemoryStore accessMemoryStore;
    private final FarmingRunStateStore farmingRunStateStore;
    private final FarmingAccessEvaluator farmingAccessEvaluator;
    private final ObservedStateStore observedStateStore;
    private String lastAccountIdentity;
    private Integer lastAccountTypeCode;

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
            LivePohStateReader pohStateReader,
            LiveSailingStateReader sailingStateReader,
            LiveEconomyReader economyReader,
            LiveCombatEvidenceReader combatEvidenceReader,
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
        this.pohStateReader = pohStateReader;
        this.sailingStateReader = sailingStateReader;
        this.economyReader = economyReader;
        this.combatEvidenceReader = combatEvidenceReader;
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
                null, null, null, null, null, null, null, null, null,
                accessMemoryStore, farmingRunStateStore,
                farmingAccessEvaluator, observedStateStore);
    }

    public synchronized GameData read()
    {
        var account = accountReader.read();
        if (account == null) return null;

        // A name is mutable and must never be used to attach cached state to a
        // character. During login/loading RuneLite can briefly lack the stable
        // hash; return no snapshot until it is available, retaining the prior
        // identity only so the next proven hash can perform the correct clear.
        if (!account.hasStableAccountIdentity()) return null;

        var identity = accountIdentity(account);
        if (lastAccountIdentity != null && !lastAccountIdentity.equals(identity))
        {
            // RuneScapeProfileChanged is the normal signal, but comparing the
            // observed character is a second fail-closed boundary for fast
            // account switches and unusual event ordering.
            clearAccountScopedCaches();
        }
        else if (lastAccountIdentity != null
                && lastAccountTypeCode != null
                && lastAccountTypeCode != account.modeCode())
        {
            // The character is stable, but bank/storage usability and build
            // routing can change with mode. Preferences/history live in the
            // RuneLite profile stores and are intentionally not cleared here.
            clearAccountScopedCaches();
        }
        lastAccountIdentity = identity;
        lastAccountTypeCode = account.modeCode();

        var inventory = itemStateReader.readInventory();
        var bank = itemStateReader.readBank();
        var equipment = itemStateReader.readEquipment();
        var rememberedStorage = observedStateStore.storage();
        StorageSnapshot storage = runePouchStateReader == null
                ? rememberedStorage
                : runePouchStateReader.merge(rememberedStorage, inventory);

        AccountEconomySnapshot liveEconomy = economyReader == null
                ? null : economyReader.read(account, inventory, bank);
        AccountEconomySnapshot economy = liveEconomy != null
                ? liveEconomy : observedStateStore.economy();

        var liveQuests = questStateReader.read();
        QuestSnapshot quests = liveQuests != null
                ? liveQuests : observedStateStore.quests();
        DiarySnapshot liveDiaries = diaryStateReader == null
                ? null : diaryStateReader.read();
        DiarySnapshot diaries = liveDiaries != null
                ? liveDiaries : observedStateStore.diaries();
        CombatAchievementSnapshot observedCombatAchievements =
                observedStateStore.combatAchievements();
        CombatAchievementSnapshot combatAchievements =
                combatAchievementReader == null
                        ? observedCombatAchievements
                        : combatAchievementReader.read(observedCombatAchievements);

        var accountMode = AccountMode.fromTypeCode(account.modeCode());
        ItemsState liveGroupStorage = accountMode.isGroupIronman()
                ? itemStateReader.readGroupStorage() : null;
        ItemsState groupStorage = liveGroupStorage != null
                ? liveGroupStorage : observedStateStore.groupStorage();
        var rememberedClue = observedStateStore.clue();
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
                ? liveSlayer : observedStateStore.slayer();
        SailingSnapshot liveSailing = sailingStateReader == null
                ? null : sailingStateReader.read(quests);
        SailingSnapshot sailing = liveSailing != null
                ? liveSailing : observedStateStore.sailing();

        var observedPvm = observedStateStore.pvm();
        PvmSnapshot pvm = pvmReadinessAnalyzer == null
                ? observedPvm
                : pvmReadinessAnalyzer.analyze(
                        account, quests, equipment, inventory, storage, bank,
                        observedPvm);
        var accessMemory = accessMemoryStore.snapshot();
        FarmingSnapshot farming = farmingAccessEvaluator.evaluate(
                account, quests, accessMemory, observedStateStore.farming());
        return GameData.builder(account)
                .inventory(inventory)
                .bank(bank)
                .equipment(equipment)
                .quests(quests)
                .diaries(diaries)
                .clue(clue)
                .combatAchievements(combatAchievements)
                .collectionLog(observedStateStore.collectionLog())
                .economy(economy)
                .capabilities(observedStateStore.capabilities())
                .accessMemory(accessMemory)
                .farmingRuns(farmingRunStateStore.snapshot())
                .storage(storage)
                .transport(observedStateStore.transport())
                .poh(observedStateStore.poh())
                .groupStorage(groupStorage)
                .slayer(slayer)
                .farming(farming)
                .sailing(sailing)
                .minigames(observedStateStore.minigames())
                .pvm(pvm)
                .recurringOpportunities(observedStateStore.recurringOpportunities())
                .combatEvidence(combatEvidenceReader == null
                        ? null : combatEvidenceReader.read())
                .build();
    }

    /** Polls only the ownership-proven build-mode scene for POH changes. */
    public synchronized boolean observePoh()
    {
        if (pohStateReader == null) return false;
        var account = accountReader.read();
        if (account == null || !account.hasStableAccountIdentity()
                || lastAccountIdentity == null
                || !lastAccountIdentity.equals(accountIdentity(account)))
            return false;
        var observed = pohStateReader.read();
        if (observed == null || observed.equals(observedStateStore.poh()))
            return false;
        observedStateStore.setPoh(observed);
        return true;
    }

    /** Reads individual diary rows only while the journal page exposes them. */
    public synchronized boolean observeOpenDiary()
    {
        return diaryStateReader != null && diaryStateReader.observeOpenDiary();
    }

    public synchronized void clearForAccountChange()
    {
        lastAccountIdentity = null;
        lastAccountTypeCode = null;
        clearAccountScopedCaches();
    }

    private void clearAccountScopedCaches()
    {
        itemStateReader.clearAccountCaches();
        if (slayerStateReader != null) slayerStateReader.clear();
        if (diaryStateReader != null) diaryStateReader.clear();
        accessMemoryStore.clearCacheForAccountChange();
        farmingRunStateStore.clearCacheForAccountChange();
        observedStateStore.clearForAccountChange();
    }

    static String accountIdentity(AccountSnapshot account)
    {
        if (account == null || !account.hasStableAccountIdentity()) return "";
        // This value is an internal equality key only. Never render or log it.
        return Long.toUnsignedString(account.getAccountHash());
    }
}
