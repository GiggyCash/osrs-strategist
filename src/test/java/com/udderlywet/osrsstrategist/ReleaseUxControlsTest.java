package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReleaseUxControlsTest
{
    @Test
    public void supportIsHiddenWhenUnconfiguredAndNeverBrowses()
    {
        AtomicInteger opened = new AtomicInteger();
        OsrsStrategistPanel panel = panel("", value -> opened.incrementAndGet(),
                () -> { });
        assertFalse(panel.isSupportVisible());
        panel.clickSupportForTest();
        assertEquals(0, opened.get());
    }

    @Test
    public void configuredSupportUsesOnlyExplicitClick()
    {
        AtomicReference<String> opened = new AtomicReference<>();
        OsrsStrategistPanel panel = panel("https://example.test/support",
                opened::set, () -> { });
        assertTrue(panel.isSupportVisible());
        assertNull(opened.get());
        panel.clickSupportForTest();
        assertEquals("https://example.test/support", opened.get());
    }

    @Test
    public void firstUseAndResetAreExplicitCallbacks()
    {
        AtomicInteger reset = new AtomicInteger();
        OsrsStrategistPanel panel = panel("", value -> { },
                reset::incrementAndGet);
        assertTrue(panel.isFirstUseHintVisible());
        panel.clickResetFeedbackForTest();
        assertEquals(1, reset.get());
        assertFalse(panel.isFirstUseHintVisible());
    }

    @Test
    public void selectedGoalFallbackIsVisibleWithoutBlankingDoNext()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        panel.updateAccount("Player", "Main", MembershipStatus.F2P, 500);
        panel.updateGoal(GoalType.BOWFA);
        panel.updateRecommendations(Collections.singletonList(recommendation()));
        assertTrue(panel.recommendationTextForTest()
                .contains("requires members content"));
        assertFalse(panel.recommendationTextForTest().trim().isEmpty());
    }

    @Test
    public void detailsControlDoesNotAffectSidebarRecommendation()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        panel.updateRecommendations(Collections.singletonList(recommendation()));
        String before = panel.recommendationTextForTest();
        panel.setDetailsOverlayEnabled(false);
        assertFalse(panel.isDetailsControlEnabled());
        assertEquals(before, panel.recommendationTextForTest());
        panel.setDetailsOverlayEnabled(true);
        assertTrue(panel.isDetailsControlEnabled());
    }

    @Test
    public void cancelledResetDoesNotEraseFeedback()
    {
        AtomicInteger reset = new AtomicInteger();
        OsrsStrategistPanel panel = new OsrsStrategistPanel(
                (id, feedback) -> { }, null, recommendation -> { },
                () -> { }, reset::incrementAndGet, () -> { }, "",
                value -> { }, () -> false);
        panel.clickResetFeedbackForTest();
        assertEquals(0, reset.get());
        assertTrue(panel.isFirstUseHintVisible());
    }

    private static OsrsStrategistPanel panel(String support,
            java.util.function.Consumer<String> browser, Runnable reset)
    {
        return new OsrsStrategistPanel((id, feedback) -> { }, null,
                recommendation -> { }, () -> { }, reset, () -> { },
                support, browser, () -> true);
    }

    private static Recommendation recommendation()
    {
        return new Recommendation("quest:test", "Complete a useful quest",
                "Unlocks useful progression.", 1.0, null,
                RecommendationConfidence.VERIFIED, 0, 0,
                new RecommendationGuidance("Start the quest.", null,
                        "Quest start", null));
    }
}
