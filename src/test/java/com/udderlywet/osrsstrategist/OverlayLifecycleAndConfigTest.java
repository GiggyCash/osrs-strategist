package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OverlayLifecycleAndConfigTest
{
    @Test
    public void allFourIndependentOverlayStatesAreSupported()
    {
        assertState(true, true);
        assertState(true, false);
        assertState(false, true);
        assertState(false, false);
    }

    @Test
    public void visibleDetailsSuppressDuplicateMethodOverlay()
    {
        OverlayDisplayState state = new OverlayDisplayState(true, true);
        assertTrue(state.showsMethodGuidance(false));
        assertFalse(state.showsMethodGuidance(true));
    }

    @Test
    public void lifecycleRegistrationAndRemovalAreIdempotent()
    {
        OverlayLifecycleGuard guard = new OverlayLifecycleGuard();
        assertTrue(guard.beginRegistration());
        assertFalse(guard.beginRegistration());
        assertTrue(guard.isRegistered());
        assertTrue(guard.beginRemoval());
        assertFalse(guard.beginRemoval());
        assertFalse(guard.isRegistered());
    }

    @Test
    public void cosmeticOverlayChangesDoNotRerank()
    {
        assertFalse(CompassConfigKeys.changesPlanning(
                CompassConfigKeys.DETAILS_OVERLAY));
        assertFalse(CompassConfigKeys.changesPlanning(
                CompassConfigKeys.METHOD_OVERLAY));
        assertTrue(CompassConfigKeys.changesPlanning(
                CompassConfigKeys.ACTIVE_GOAL));
        assertTrue(CompassConfigKeys.changesPlanning(
                CompassConfigKeys.STRATEGY_MODE));
        assertTrue(CompassConfigKeys.changesPlanning(
                CompassConfigKeys.SESSION_INTENT));
        assertTrue(CompassConfigKeys.changesStrategyProfile(
                CompassConfigKeys.ACTIVE_GOAL));
        assertFalse(CompassConfigKeys.changesStrategyProfile(
                "birdhouseReminders"));
    }

    private static void assertState(boolean details, boolean method)
    {
        OverlayDisplayState state = new OverlayDisplayState(details, method);
        if (details) assertTrue(state.showsDetails());
        else assertFalse(state.showsDetails());
        if (method) assertTrue(state.showsMethodGuidance());
        else assertFalse(state.showsMethodGuidance());
    }
}
