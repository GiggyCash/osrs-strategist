package compass;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UiGenerationGuardTest
{
    @Test
    public void rapidRefreshAndShutdownRejectQueuedStaleWork()
    {
        UiGenerationGuard guard = new UiGenerationGuard();
        long login = guard.next();
        long accountSwitch = guard.next();
        assertFalse(guard.isCurrent(login));
        assertTrue(guard.isCurrent(accountSwitch));

        guard.invalidate();
        assertFalse(guard.isCurrent(accountSwitch));
    }

    @Test
    public void onlyNewestOfManyQueuedAccountEventsMayRender()
    {
        UiGenerationGuard guard = new UiGenerationGuard();
        long[] events = new long[250];
        for (int i = 0; i < events.length; i++) events[i] = guard.next();
        for (int i = 0; i < events.length - 1; i++)
            assertFalse("stale event " + i, guard.isCurrent(events[i]));
        assertTrue(guard.isCurrent(events[events.length - 1]));
    }

    @Test
    public void shutdownInvalidatesLoadingAndFallbackUpdates()
    {
        UiGenerationGuard guard = new UiGenerationGuard();
        long login = guard.next();
        long loadingFallback = guard.next();
        assertFalse(guard.isCurrent(login));
        guard.invalidate();
        assertFalse(guard.isCurrent(loadingFallback));
    }

    @Test
    public void thousandsOfBenignRefreshesRetainOnlyOneCurrentGeneration()
    {
        UiGenerationGuard guard = new UiGenerationGuard();
        long previous = 0;
        for (int i = 0; i < 2_000; i++)
        {
            long current = guard.next();
            if (previous != 0) assertFalse(guard.isCurrent(previous));
            previous = current;
        }
        assertTrue(guard.isCurrent(previous));
    }
}
