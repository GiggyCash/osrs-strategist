package compass;

import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/** Event-sequence regression coverage for account-scoped observations. */
public class ObservedStateStoreLifecycleTest
{
    @Test
    public void accountSwitchClearsEveryHighValueObservation()
    {
        ObservedStateStore store = new ObservedStateStore();
        store.setQuests(new QuestSnapshot(Collections.emptyMap()));
        store.setGroupStorage(new ItemsState(true,
                Collections.singletonList(new ItemState(1, "item", 50))));
        store.setSlayer(TestFixtures.slayerSnapshot("Abyssal demons", 100,
                "Duradel", 200, Confidence.VERIFIED));
        store.setPvm(PvmSnapshot.unknown());
        store.setRecurringOpportunities(new RecurringOpportunitySnapshot(
                Collections.singletonMap("herb-run", 1L)));
        store.setStorage(StorageSnapshot.unknown());

        store.clearForAccountChange();

        assertNull(store.quests());
        assertNull(store.groupStorage());
        assertNull(store.slayer());
        assertNull(store.pvm());
        assertNull(store.recurringOpportunities());
        assertNull(store.storage());
        assertEquals(Capability.UNKNOWN,
                store.capabilities().get("bank-observed"));
    }

    @Test
    public void rapidSwitchCannotRestorePreviousAccountSnapshot()
    {
        ObservedStateStore store = new ObservedStateStore();
        QuestSnapshot first = new QuestSnapshot(Collections.singletonMap(
                "Cook's Assistant", QuestStatus.COMPLETE));
        QuestSnapshot second = new QuestSnapshot(Collections.singletonMap(
                "Cook's Assistant", QuestStatus.NOT_STARTED));
        store.setQuests(first);
        store.clearForAccountChange();
        store.setQuests(second);

        assertSame(second, store.quests());
    }

    @Test
    public void repeatedAccountChangesNeverRetainBankAdjacentTaskOrOpportunityState()
    {
        ObservedStateStore store = new ObservedStateStore();
        for (int i = 0; i < 100; i++)
        {
            store.setGroupStorage(new ItemsState(true,
                    Collections.singletonList(new ItemState(
                            i + 1, "Account item", 1))));
            store.setSlayer(TestFixtures.slayerSnapshot("Task " + i, i + 1,
                    "Master", 0, Confidence.VERIFIED));
            store.setRecurringOpportunities(new RecurringOpportunitySnapshot(
                    Collections.singletonMap("ready:" + i, (long) i)));
            store.clearForAccountChange();
            assertNull(store.groupStorage());
            assertNull(store.slayer());
            assertNull(store.recurringOpportunities());
        }
    }
}
