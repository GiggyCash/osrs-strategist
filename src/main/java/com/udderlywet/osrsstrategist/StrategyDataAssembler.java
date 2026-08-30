package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Builds one immutable {@link StrategyDataBundle} from live RuneLite state and
 * previously verified observations.
 *
 * <p>This is the seam between "reading the game" and "reasoning about the
 * game". Strategy code should not reach into RuneLite directly.</p>
 */
@Singleton
public class StrategyDataAssembler
{
    private final AccountReader accountReader;
    private final LiveItemStateReader itemStateReader;
    private final LiveQuestStateReader questStateReader;
    private final AccountAccessMemoryStore accessMemoryStore;
    private final FarmingAccessEvaluator farmingAccessEvaluator;
    private final ObservedStateStore observedStateStore;

    @Inject
    public StrategyDataAssembler(
            AccountReader accountReader,
            LiveItemStateReader itemStateReader,
            LiveQuestStateReader questStateReader,
            AccountAccessMemoryStore accessMemoryStore,
            FarmingAccessEvaluator farmingAccessEvaluator,
            ObservedStateStore observedStateStore)
    {
        this.accountReader = accountReader;
        this.itemStateReader = itemStateReader;
        this.questStateReader = questStateReader;
        this.accessMemoryStore = accessMemoryStore;
        this.farmingAccessEvaluator = farmingAccessEvaluator;
        this.observedStateStore = observedStateStore;
    }

    public StrategyDataBundle read()
    {
        AccountSnapshot account = accountReader.read();

        if (account == null)
        {
            return null;
        }

        QuestSnapshot liveQuests = questStateReader.read();
        QuestSnapshot quests = liveQuests != null
                ? liveQuests
                : observedStateStore.getQuests();

        AccessMemorySnapshot accessMemory = accessMemoryStore.snapshot();
        FarmingSnapshot farming = farmingAccessEvaluator.evaluate(
                account,
                quests,
                accessMemory,
                observedStateStore.getFarming()
        );

        return StrategyDataBundle.builder(account)
                .inventory(itemStateReader.readInventory())
                .bank(itemStateReader.readBank())
                .equipment(itemStateReader.readEquipment())
                .quests(quests)
                .diaries(observedStateStore.getDiaries())
                .clue(observedStateStore.getClue())
                .combatAchievements(
                        observedStateStore.getCombatAchievements()
                )
                .collectionLog(observedStateStore.getCollectionLog())
                .economy(observedStateStore.getEconomy())
                .capabilities(observedStateStore.getCapabilities())
                .accessMemory(accessMemory)
                .storage(observedStateStore.getStorage())
                .transport(observedStateStore.getTransport())
                .poh(observedStateStore.getPoh())
                .groupStorage(observedStateStore.getGroupStorage())
                .slayer(observedStateStore.getSlayer())
                .farming(farming)
                .sailing(observedStateStore.getSailing())
                .minigames(observedStateStore.getMinigames())
                .pvm(observedStateStore.getPvm())
                .recurringOpportunities(
                        observedStateStore.getRecurringOpportunities()
                )
                .build();
    }

    /**
     * Must be called when the active RuneScape profile changes so bank and
     * in-memory observations never leak between characters. Persistent access
     * memory is managed separately by AccountAccessMemoryStore.
     */
    public void clearForAccountChange()
    {
        itemStateReader.clearAccountCaches();
        observedStateStore.clearForAccountChange();
    }
}
