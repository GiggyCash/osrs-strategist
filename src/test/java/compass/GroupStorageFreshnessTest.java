package compass;

import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GroupStorageFreshnessTest
{
    @Test
    public void recentObservationCountsAndStaleObservationFailsClosed()
    {
        long now = System.currentTimeMillis();
        ItemsState recent = new ItemsState(true,
                Collections.singletonList(new ItemState(
                        1, "Shared item", 1)), now);
        ItemsState stale = new ItemsState(true,
                Collections.singletonList(new ItemState(
                        1, "Shared item", 1)),
                now - ItemsState.FRESH_FOR_MILLIS - 1L);

        assertTrue(recent.isObserved());
        assertTrue(recent.containsItem(1));
        assertFalse(stale.isObserved());
        assertFalse(stale.containsItem(1));
        assertFalse(ItemsState.unknown().isObserved());
    }

    @Test
    public void futureDatedObservationFailsClosed()
    {
        ItemsState future = new ItemsState(true,
                Collections.emptyList(),
                System.currentTimeMillis() + 60_000L);
        assertFalse(future.isObserved());
    }
}
