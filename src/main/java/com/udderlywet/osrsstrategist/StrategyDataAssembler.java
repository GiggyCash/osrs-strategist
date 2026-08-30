package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Builds one immutable {@link StrategyDataBundle} from live RuneLite state and
 * previously verified observations.
 *
 * <p>This is the seam between "reading the game" and "reasoning about the
 * game". Strategy code should not reach into RuneLite directly. That separation
 * is what will let us unit-test Main, Ironman, GIM, UIM, and future edge cases
 * with fake snapshots.</p>
 */
@Singleton
public class StrategyDataAssembler
{
    private final AccountReader accountReader;
    private final LiveItemStateReader itemStateReader;
    private final ObservedStateStore observedStateStore;

    @Inject
    public StrategyDataAssembler(
            AccountReader accountReader,
            LiveItemStateReader itemStateReader,
            ObservedStateStore observedStateStore)
    {
        this.accountReader = accountReader;
        this.itemStateReader = itemStateReader;
        this.observedStateStore = observedStateStore;
    }

    public StrategyDataBundle read()
    {
        AccountSnapshot account = accountReader.read();

        if (account == null)
        {
            return null;
        }

        return StrategyDataBundle.builder(account)
                .inventory(itemStateReader.readInventory())
                .bank(itemStateReader.readBank())
                .equipment(itemStateReader.readEquipment())
                .quests(observedStateStore.getQuests())
                .diaries(observedStateStore.getDiaries())
                .clue(observedStateStore.getClue())
                .combatAchievements(
                        observedStateStore.getCombatAchievements()
                )
                .collectionLog(observedStateStore.getCollectionLog())
                .economy(observedStateStore.getEconomy())
                .capabilities(observedStateStore.getCapabilities())
                .storage(observedStateStore.getStorage())
                .transport(observedStateStore.getTransport())
                .poh(observedStateStore.getPoh())
                .groupStorage(observedStateStore.getGroupStorage())
                .slayer(observedStateStore.getSlayer())
                .farming(observedStateStore.getFarming())
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
     * observation caches never leak between characters.
     */
    public void clearForAccountChange()
    {
        itemStateReader.clearAccountCaches();
        observedStateStore.clearForAccountChange();
    }
}
