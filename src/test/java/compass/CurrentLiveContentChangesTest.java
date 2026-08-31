package compass;

import java.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

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
        assertTrue(CurrentLiveContentChanges.mayAffectPlanning(
                "2026-08-12-colossal-wyrm-courses", VALIDATED));
        assertTrue(CurrentLiveContentChanges.mayAffectPlanning(
                "2026-08-12-agility-shortcuts", VALIDATED));
        assertTrue(CurrentLiveContentChanges.mayAffectPlanning(
                "2026-08-19-birdhouse-xp", VALIDATED));
        assertTrue(CurrentLiveContentChanges.mayAffectPlanning(
                "2026-08-19-hunter-methods", VALIDATED));
    }

    @Test
    public void announcedFutureChangesCannotAffectCurrentPlanning()
    {
        assertFalse(CurrentLiveContentChanges.mayAffectPlanning(
                "2026-09-02-sweep-up-follow-up", VALIDATED));
        assertFalse(CurrentLiveContentChanges.mayAffectPlanning(
                "unknown-change", VALIDATED));
    }

    @Test
    public void lifecycleVocabularyIncludesUnknownAndSuperseded()
    {
        assertEquals(4, CurrentLiveContentChanges.Status.values().length);
        assertTrue(java.util.Arrays.asList(
                CurrentLiveContentChanges.Status.values()).contains(
                        CurrentLiveContentChanges.Status.UNKNOWN));
        assertTrue(java.util.Arrays.asList(
                CurrentLiveContentChanges.Status.values()).contains(
                        CurrentLiveContentChanges.Status.REMOVED_SUPERSEDED));
    }
}
