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
    public void negativeFeedbackIsTheOnlyPrimaryFeedback()
    {
        OsrsStrategistPanel panel = panel("", value -> { },
                () -> { });
        assertEquals(java.util.Arrays.asList("Later", "Not Today", "Dislike"),
                panel.feedbackLabelsForTest());
        assertFalse(panel.feedbackLabelsForTest().contains("Do This"));
    }

    @Test
    public void selectedGoalFallbackIsVisibleWithoutBlankingDoNext()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        panel.updateAccount("Player", "Main", MembershipStatus.F2P, 500);
        panel.updateGoal(GoalType.BOWFA);
        panel.updateRecommendations(Collections.singletonList(recommendation()));
        assertFalse(panel.recommendationTextForTest().contains("GOAL"));
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
        assertFalse(panel.isDetailsControlVisible());
        assertEquals(before, panel.recommendationTextForTest());
        panel.setDetailsOverlayEnabled(true);
        assertTrue(panel.isDetailsControlEnabled());
        assertTrue(panel.isDetailsControlVisible());
    }

    @Test
    public void emptySecondarySectionsDoNotOccupyTheSidebar()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        panel.updateRecommendations(Collections.singletonList(recommendation()));
        panel.updateOpportunities(Collections.emptyList());
        assertFalse(panel.areAlternativesVisibleForTest());
        assertFalse(panel.areOpportunitiesVisibleForTest());
    }

    @Test
    public void alternativesUseCompactLevelAndMethodLines()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        Recommendation first = recommendation("skill:mining", "Train Mining to 40");
        Recommendation second = recommendation("skill:fishing", "Train Fishing to 40");
        panel.updateRecommendations(java.util.Arrays.asList(first, second));

        String text = panel.firstAlternativeTextForTest();
        assertTrue(text.contains("Concrete method"));
        assertTrue(text.contains("30 → 40"));
        assertFalse(text.contains("Named location"));
    }

    @Test
    public void fallbackHidesControlsThatCannotDoAnything()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        panel.updateRecommendations(Collections.emptyList());

        assertFalse(panel.isDetailsControlVisible());
        assertFalse(panel.isFeedbackVisibleForTest());
        assertFalse(panel.isProgressVisibleForTest());
    }

    @Test
    public void opportunityCardExplainsFirstPreparationInsteadOfDebugConfidence()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        Opportunity herb = new Opportunity("opportunity:herb-run",
                OpportunityType.HERB_RUN, "Herb run", true,
                RecommendationConfidence.VERIFIED,
                Collections.singletonList("Carry a spade"), false,
                CandidateSafetyEvidence.skill(false,
                        net.runelite.api.Skill.FARMING));

        panel.updateOpportunities(Collections.singletonList(herb));

        assertTrue(panel.firstOpportunityTextForTest()
                .contains("Prep: Carry a spade"));
        assertFalse(panel.firstOpportunityTextForTest()
                .contains("Check Needed"));
    }

    private static OsrsStrategistPanel panel(String support,
            java.util.function.Consumer<String> browser, Runnable reset)
    {
        return new OsrsStrategistPanel((id, feedback) -> { }, null,
                recommendation -> { }, () -> { }, () -> { },
                support, browser);
    }

    private static Recommendation recommendation()
    {
        return new Recommendation("quest:test", "Complete a useful quest",
                "Unlocks useful progression.", 1.0, null,
                RecommendationConfidence.VERIFIED, 0, 0,
                new RecommendationGuidance("Start the quest.", null,
                        "Quest start", null));
    }

    private static Recommendation recommendation(String id, String title)
    {
        net.runelite.api.Skill skill = id.contains("fishing")
                ? net.runelite.api.Skill.FISHING : net.runelite.api.Skill.MINING;
        TrainingMethod method = new TrainingMethod(id + ":method", skill,
                1, 99, "Concrete method", "Named location", 1, 1, 1,
                AttentionLevel.MODERATE, 10, 1, Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        return new Recommendation(id, title, "Reason", 1,
                new TrainingPlan(method, "Reason",
                        RecommendationConfidence.VERIFIED),
                RecommendationConfidence.VERIFIED, 30, 40,
                new RecommendationGuidance("Repeat the exact loop.",
                        "Required tool.", "Named location.", null));
    }
}
