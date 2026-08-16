package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Builds one immutable StrategyDataBundle from live and remembered evidence. */
@Singleton
public class StrategyDataAssembler
{
    private final AccountReader accountReader;
    private final LiveItemStateReader itemStateReader;
    private final LiveQuestStateReader questStateReader;
    private final LiveDiaryStateReader diaryStateReader;
    private final AccountAccessMemoryStore accessMemoryStore;
    private final FarmingRunStateStore farmingRunStateStore;
    private final FarmingAccessEvaluator farmingAccessEvaluator;
    private final ObservedStateStore observedStateStore;

    @Inject
    public StrategyDataAssembler(
            AccountReader accountReader,
            LiveItemStateReader itemStateReader,
            LiveQuestStateReader questStateReader,
            LiveDiaryStateReader diaryStateReader,
            AccountAccessMemoryStore accessMemoryStore,
            FarmingRunStateStore farmingRunStateStore,
            FarmingAccessEvaluator farmingAccessEvaluator,
            ObservedStateStore observedStateStore)
    {
        this.accountReader = accountReader;
        this.itemStateReader = itemStateReader;
        this.questStateReader = questStateReader;
        this.diaryStateReader = diaryStateReader;
        this.accessMemoryStore = accessMemoryStore;
        this.farmingRunStateStore = farmingRunStateStore;
        this.farmingAccessEvaluator = farmingAccessEvaluator;
        this.observedStateStore = observedStateStore;
    }

    public StrategyDataBundle read()
    {
        AccountSnapshot account = accountReader.read();
        if (account == null) return null;

        QuestSnapshot liveQuests = questStateReader.read();
        QuestSnapshot quests = liveQuests != null
                ? liveQuests : observedStateStore.getQuests();
        DiarySnapshot liveDiaries = diaryStateReader.read();
        DiarySnapshot diaries = liveDiaries != null
                ? liveDiaries : observedStateStore.getDiaries();
        AccessMemorySnapshot accessMemory = accessMemoryStore.snapshot();
        FarmingSnapshot farming = farmingAccessEvaluator.evaluate(
                account, quests, accessMemory, observedStateStore.getFarming());

        return StrategyDataBundle.builder(account)
                .inventory(itemStateReader.readInventory())
                .bank(itemStateReader.readBank())
                .equipment(itemStateReader.readEquipment())
                .quests(quests)
                .diaries(diaries)
                .clue(observedStateStore.getClue())
                .combatAchievements(observedStateStore.getCombatAchievements())
                .collectionLog(observedStateStore.getCollectionLog())
                .economy(observedStateStore.getEconomy())
                .capabilities(observedStateStore.getCapabilities())
                .accessMemory(accessMemory)
                .farmingRuns(farmingRunStateStore.snapshot())
                .storage(observedStateStore.getStorage())
                .transport(observedStateStore.getTransport())
                .poh(observedStateStore.getPoh())
                .groupStorage(observedStateStore.getGroupStorage())
                .slayer(observedStateStore.getSlayer())
                .farming(farming)
                .sailing(observedStateStore.getSailing())
                .minigames(observedStateStore.getMinigames())
                .pvm(observedStateStore.getPvm())
                .recurringOpportunities(observedStateStore.getRecurringOpportunities())
                .build();
    }

    public void clearForAccountChange()
    {
        itemStateReader.clearAccountCaches();
        observedStateStore.clearForAccountChange();
    }
}
