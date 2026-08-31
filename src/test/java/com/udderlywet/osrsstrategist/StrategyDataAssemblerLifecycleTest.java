package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class StrategyDataAssemblerLifecycleTest
{
    @Test
    public void stableIdentitySurvivesRenameMembershipAndModeTransitions()
    {
        AccountSnapshot mainF2p = account("Alice", 17L, 0, MembershipStatus.F2P);
        AccountSnapshot renamed = account("New Alice", 17L, 0, MembershipStatus.P2P);
        AccountSnapshot iron = account("New Alice", 17L, 1, MembershipStatus.P2P);
        AccountSnapshot sameNameOther = account("New Alice", 99L, 0, MembershipStatus.P2P);

        assertEquals(StrategyDataAssembler.accountIdentity(mainF2p),
                StrategyDataAssembler.accountIdentity(renamed));
        assertEquals(StrategyDataAssembler.accountIdentity(renamed),
                StrategyDataAssembler.accountIdentity(iron));
        assertNotEquals(StrategyDataAssembler.accountIdentity(renamed),
                StrategyDataAssembler.accountIdentity(sameNameOther));
    }

    @Test
    public void unavailableStableIdentityCannotFormAnAccountKey()
    {
        assertEquals("", StrategyDataAssembler.accountIdentity(
                account("Loading", 0L, 0, MembershipStatus.UNKNOWN)));
    }

    @Test
    public void renamePreservesObservedBankSlayerOpportunityAndUimGimState()
    {
        MutableAccountReader accounts = new MutableAccountReader(
                account("Old name", 44L, 4, MembershipStatus.P2P));
        FakeItems items = new FakeItems();
        ObservedStateStore observed = populatedObservedState();
        StrategyDataAssembler assembler = assembler(accounts, items, observed);

        GameData before = assembler.read();
        accounts.account = account("New name", 44L, 4, MembershipStatus.P2P);
        GameData after = assembler.read();

        assertSame(before.bank(), after.bank());
        assertSame(before.slayer(), after.slayer());
        assertSame(before.recurringOpportunities(), after.recurringOpportunities());
        assertSame(before.groupStorage(), after.groupStorage());
        assertSame(before.storage(), after.storage());
        assertEquals(0, items.clears);
    }

    @Test
    public void differentHashClearsEvenWhenNameMatchesAndLoadingCannotLeak()
    {
        MutableAccountReader accounts = new MutableAccountReader(
                account("Same name", 44L, 0, MembershipStatus.P2P));
        FakeItems items = new FakeItems();
        ObservedStateStore observed = populatedObservedState();
        StrategyDataAssembler assembler = assembler(accounts, items, observed);
        assembler.read();
        SlayerSnapshot priorSlayer = observed.slayer();

        accounts.account = account("Same name", 0L, 0, MembershipStatus.UNKNOWN);
        assertNull(assembler.read());
        assertSame(priorSlayer, observed.slayer());

        accounts.account = account("Same name", 55L, 0, MembershipStatus.P2P);
        GameData switched = assembler.read();
        assertNull(switched.slayer());
        assertNull(switched.recurringOpportunities());
        assertNull(switched.groupStorage());
        assertNull(switched.storage());
        assertEquals(1, items.clears);
    }

    @Test
    public void modeTransitionKeepsCharacterIdentityButInvalidatesModeSensitiveState()
    {
        MutableAccountReader accounts = new MutableAccountReader(
                account("Mode", 71L, 4, MembershipStatus.P2P));
        FakeItems items = new FakeItems();
        ObservedStateStore observed = populatedObservedState();
        StrategyDataAssembler assembler = assembler(accounts, items, observed);
        assembler.read();

        accounts.account = account("Mode", 71L, 2, MembershipStatus.P2P);
        GameData transitioned = assembler.read();
        assertNull(transitioned.groupStorage());
        assertNull(transitioned.storage());
        assertNull(transitioned.slayer());
        assertEquals(1, items.clears);
    }

    @Test
    public void liveGroupStorageObservationIsUsedOnlyForCurrentGim()
    {
        MutableAccountReader accounts = new MutableAccountReader(
                account("GIM A", 81L, 4, MembershipStatus.P2P));
        FakeItems items = new FakeItems();
        items.group = new ItemsState(true,
                Collections.singletonList(new ItemState(
                        55, "Shared plank", 12)));
        StrategyDataAssembler assembler = assembler(accounts, items,
                new ObservedStateStore());

        GameData first = assembler.read();
        assertSame(items.group, first.groupStorage());
        assertEquals(12, first.groupStorage().getItems().get(0).getQuantity());

        accounts.account = account("Main B", 82L, 0, MembershipStatus.P2P);
        GameData switched = assembler.read();
        assertNull(switched.groupStorage());
    }

    private static StrategyDataAssembler assembler(MutableAccountReader accounts,
            FakeItems items, ObservedStateStore observed)
    {
        return new StrategyDataAssembler(accounts, items, new EmptyQuests(),
                new EmptyAccessStore(), new EmptyFarmingRuns(),
                new PassthroughFarming(), observed);
    }

    private static ObservedStateStore populatedObservedState()
    {
        ObservedStateStore observed = new ObservedStateStore();
        observed.setSlayer(new SlayerSnapshot("Goblins", 12, "Turael", 0,
                Confidence.VERIFIED));
        Map<String, Long> ready = new HashMap<>();
        ready.put("herb-run", 1L);
        observed.setRecurringOpportunities(new RecurringOpportunitySnapshot(ready));
        observed.setGroupStorage(new ItemsState(true, Collections.emptyList()));
        observed.setStorage(StorageSnapshot.unknown());
        return observed;
    }

    private static final class MutableAccountReader extends AccountReader
    {
        private AccountSnapshot account;
        private MutableAccountReader(AccountSnapshot account) { super(null); this.account = account; }
        @Override public AccountSnapshot read() { return account; }
    }

    private static final class FakeItems extends LiveItemStateReader
    {
        private final ItemsState bank = new ItemsState(Collections.emptyList(), 1L);
        private ItemsState group;
        private int clears;
        private FakeItems() { super(null, null); }
        @Override public ItemsState readInventory() { return new ItemsState(Collections.emptyList()); }
        @Override public ItemsState readBank() { return bank; }
        @Override public ItemsState readEquipment() { return new ItemsState(Collections.emptyList()); }
        @Override public ItemsState readGroupStorage() { return group; }
        @Override public void clearAccountCaches() { clears++; group = null; }
    }

    private static final class EmptyQuests extends LiveQuestStateReader
    {
        private EmptyQuests() { super(null); }
        @Override public QuestSnapshot read() { return null; }
    }

    private static final class EmptyAccessStore extends AccountAccessMemoryStore
    {
        private EmptyAccessStore() { super(null, null); }
        @Override public AccessMemorySnapshot snapshot() { return AccessMemorySnapshot.empty(); }
        @Override public void clearCacheForAccountChange() { }
    }

    private static final class EmptyFarmingRuns extends FarmingRunStateStore
    {
        private EmptyFarmingRuns() { super(null, null); }
        @Override public FarmingRunSnapshot snapshot() { return FarmingRunSnapshot.empty(); }
        @Override public void clearCacheForAccountChange() { }
    }

    private static final class PassthroughFarming extends FarmingAccessEvaluator
    {
        private PassthroughFarming() { super(null); }
        @Override public FarmingSnapshot evaluate(AccountSnapshot account, QuestSnapshot quests,
                AccessMemorySnapshot memory, FarmingSnapshot existing) { return existing; }
    }

    private static AccountSnapshot account(String name, long hash, int type,
            MembershipStatus membership)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(name, hash, type, "Test", membership,
                1, 32, 0L, levels, xp);
    }
}
