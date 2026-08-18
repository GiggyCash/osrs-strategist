package com.udderlywet.osrsstrategist;

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
        store.setGroupStorage(new GroupStorageSnapshot(true,
                Collections.singletonList(new ItemStackSnapshot(1, "item", 50))));
        store.setSlayer(new SlayerSnapshot("Abyssal demons", 100,
                "Duradel", 200, RecommendationConfidence.VERIFIED));
        store.setPvm(PvmSnapshot.unknown());
        store.setRecurringOpportunities(new RecurringOpportunitySnapshot(
                Collections.singletonMap("herb-run", 1L)));
        store.setStorage(StorageSnapshot.unknown());

        store.clearForAccountChange();

        assertNull(store.getQuests());
        assertNull(store.getGroupStorage());
        assertNull(store.getSlayer());
        assertNull(store.getPvm());
        assertNull(store.getRecurringOpportunities());
        assertNull(store.getStorage());
        assertEquals(CapabilityState.UNKNOWN,
                store.getCapabilities().get("bank-observed"));
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

        assertSame(second, store.getQuests());
    }
}
