package com.udderlywet.osrsstrategist;

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
}
