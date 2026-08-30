package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/**
 * In-memory home for account observations that arrive at different times.
 *
 * <p>RuneLite does not expose every piece of account state continuously. A
 * bank snapshot may be seen when the bank is open, a clue may be detected from
 * another plugin/event, and a POH capability may be confirmed by the player.
 * This store lets those observations survive normal panel refreshes while still
 * being cleared when the active character changes.</p>
 *
 * <p>No setter invents data. Callers are responsible for only storing values
 * they actually observed or the player explicitly confirmed.</p>
 */
@Singleton
public class ObservedStateStore
{
    private QuestSnapshot quests;
    private DiarySnapshot diaries;
    private ClueSnapshot clue;
    private CombatAchievementSnapshot combatAchievements;
    private CollectionLogSnapshot collectionLog;
    private AccountEconomySnapshot economy;
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
    private AccountCapabilities capabilities = new AccountCapabilities();

    public synchronized void clearForAccountChange()
    {
        quests = null;
        diaries = null;
        clue = null;
        combatAchievements = null;
        collectionLog = null;
        economy = null;
        storage = null;
        transport = null;
        poh = null;
        groupStorage = null;
        slayer = null;
        farming = null;
        sailing = null;
        minigames = null;
        pvm = null;
        recurringOpportunities = null;
        capabilities = new AccountCapabilities();
    }

    public synchronized QuestSnapshot getQuests() { return quests; }
    public synchronized void setQuests(QuestSnapshot value) { quests = value; }
    public synchronized DiarySnapshot getDiaries() { return diaries; }
    public synchronized void setDiaries(DiarySnapshot value) { diaries = value; }
    public synchronized ClueSnapshot getClue() { return clue; }
    public synchronized void setClue(ClueSnapshot value) { clue = value; }
    public synchronized CombatAchievementSnapshot getCombatAchievements() { return combatAchievements; }
    public synchronized void setCombatAchievements(CombatAchievementSnapshot value) { combatAchievements = value; }
    public synchronized CollectionLogSnapshot getCollectionLog() { return collectionLog; }
    public synchronized void setCollectionLog(CollectionLogSnapshot value) { collectionLog = value; }
    public synchronized AccountEconomySnapshot getEconomy() { return economy; }
    public synchronized void setEconomy(AccountEconomySnapshot value) { economy = value; }
    public synchronized StorageSnapshot getStorage() { return storage; }
    public synchronized void setStorage(StorageSnapshot value) { storage = value; }
    public synchronized TransportSnapshot getTransport() { return transport; }
    public synchronized void setTransport(TransportSnapshot value) { transport = value; }
    public synchronized PohSnapshot getPoh() { return poh; }
    public synchronized void setPoh(PohSnapshot value) { poh = value; }
    public synchronized GroupStorageSnapshot getGroupStorage() { return groupStorage; }
    public synchronized void setGroupStorage(GroupStorageSnapshot value) { groupStorage = value; }
    public synchronized SlayerSnapshot getSlayer() { return slayer; }
    public synchronized void setSlayer(SlayerSnapshot value) { slayer = value; }
    public synchronized FarmingSnapshot getFarming() { return farming; }
    public synchronized void setFarming(FarmingSnapshot value) { farming = value; }
    public synchronized SailingSnapshot getSailing() { return sailing; }
    public synchronized void setSailing(SailingSnapshot value) { sailing = value; }
    public synchronized MinigameSnapshot getMinigames() { return minigames; }
    public synchronized void setMinigames(MinigameSnapshot value) { minigames = value; }
    public synchronized PvmSnapshot getPvm() { return pvm; }
    public synchronized void setPvm(PvmSnapshot value) { pvm = value; }
    public synchronized RecurringOpportunitySnapshot getRecurringOpportunities() { return recurringOpportunities; }
    public synchronized void setRecurringOpportunities(RecurringOpportunitySnapshot value) { recurringOpportunities = value; }
    public synchronized AccountCapabilities getCapabilities() { return capabilities; }
}
