package com.udderlywet.osrsstrategist;

import java.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CurrentLiveContentChangesTest
{
    private static final LocalDate VALIDATED = LocalDate.of(2026, 8, 25);

    @Test
    public void liveSummerSweepUpChangesAreActiveAtValidationDate()
    {
        assertTrue(CurrentLiveContentChanges.mayAffectPlanning(
                "2026-08-12-sepulchre-floor-4", VALIDATED));
        assertTrue(CurrentLiveContentChanges.mayAffectPlanning(
                "2026-08-12-sepulchre-floor-5", VALIDATED));
        assertTrue(CurrentLiveContentChanges.mayAffectPlanning(
                "2026-08-19-birdhouse-nests", VALIDATED));
    }

    @Test
    public void announcedFutureChangesCannotAffectCurrentPlanning()
    {
        assertFalse(CurrentLiveContentChanges.mayAffectPlanning(
                "2026-09-02-sweep-up-follow-up", VALIDATED));
        assertFalse(CurrentLiveContentChanges.mayAffectPlanning(
                "unknown-change", VALIDATED));
    }
}
