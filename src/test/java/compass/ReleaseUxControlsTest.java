package compass;

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
        assertFalse(panel.supportButton.isVisible());
        panel.supportButton.doClick();
        assertEquals(0, opened.get());
    }

    @Test
    public void configuredSupportUsesOnlyExplicitClick()
    {
        AtomicReference<String> opened = new AtomicReference<>();
        OsrsStrategistPanel panel = panel("https://example.test/support",
                opened::set, () -> { });
        assertTrue(panel.supportButton.isVisible());
        assertNull(opened.get());
        panel.supportButton.doClick();
        assertEquals("https://example.test/support", opened.get());
    }

    @Test
    public void negativeFeedbackIsTheOnlyPrimaryFeedback()
    {
        OsrsStrategistPanel panel = panel("", value -> { },
                () -> { });
        assertEquals(java.util.Arrays.asList("Later", "Not Today", "Dislike"),
                java.util.Arrays.asList(panel.laterButton.getText(), panel.notTodayButton.getText(), panel.dislikeButton.getText()));
        assertFalse(java.util.Arrays.asList(panel.laterButton.getText(), panel.notTodayButton.getText(), panel.dislikeButton.getText()).contains("Do This"));
    }

    @Test
    public void selectedGoalFallbackIsVisibleWithoutBlankingDoNext()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        panel.updateAccount("Player", "Main", Membership.F2P, 500);
        panel.updateGoal(GoalType.BOWFA);
        panel.updateRecommendations(Collections.singletonList(recommendation()));
        assertFalse(panel.recommendationBody.getText().contains("GOAL"));
        assertFalse(panel.recommendationBody.getText().trim().isEmpty());
    }

    @Test
    public void detailsControlDoesNotAffectSidebarRecommendation()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        panel.updateRecommendations(Collections.singletonList(recommendation()));
        String before = panel.recommendationBody.getText();
        panel.setDetailsOverlayEnabled(false);
        assertFalse(panel.detailsButton.isEnabled());
        assertFalse(panel.detailsButton.isVisible());
        assertEquals(before, panel.recommendationBody.getText());
        panel.setDetailsOverlayEnabled(true);
        assertTrue(panel.detailsButton.isEnabled());
        assertTrue(panel.detailsButton.isVisible());
    }

    @Test
    public void dangerousStorageUsesAnExplicitRiskStepControl()
    {
        AtomicReference<Recommendation> shown = new AtomicReference<>();
        OsrsStrategistPanel panel = new OsrsStrategistPanel(
                (id, feedback) -> { }, null, shown::set,
                () -> { }, () -> { }, "", value -> { });
        Guidance guidance = new Guidance(
                "Follow only the verified retrieval sequence.",
                "Exact observed setup", "Hespori cave", "Second deaths can delete stored items.",
                BankingMode.UNKNOWN, new UimStorageDecision(
                        StorageKind.HESPORI_ITEM_RETRIEVAL, true,
                        Confidence.VERIFIED, RiskLevel.HIGH,
                        "Verified exact service"),
                RecommendationRiskDisclosure.deathStorage());
        Recommendation dangerous = new Recommendation(
                "uim:hespori-transition", "Death-storage transition",
                "A major transition is otherwise blocked.", 1.0, null,
                Confidence.CHECK_NEEDED, 0, 0, guidance,
                Safety.harmless(false));

        panel.updateRecommendations(Collections.singletonList(dangerous));
        assertEquals("View Risk Steps", panel.detailsButton.getText());
        assertNull(shown.get());
        panel.detailsButton.doClick();
        assertEquals(dangerous, shown.get());
        assertEquals("Hide Risk Steps", panel.detailsButton.getText());
    }

    @Test
    public void emptySecondarySectionsDoNotOccupyTheSidebar()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        panel.updateRecommendations(Collections.singletonList(recommendation()));
        panel.updateOpportunities(Collections.emptyList());
        assertFalse(panel.alternativesCard.isVisible());
        assertFalse(panel.opportunitiesCard.isVisible());
    }

    @Test
    public void alternativesUseCompactLevelAndMethodLines()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        Recommendation first = recommendation("skill:mining", "Train Mining to 40");
        Recommendation second = recommendation("skill:fishing", "Train Fishing to 40");
        panel.updateRecommendations(java.util.Arrays.asList(first, second));

        String text = panel.alternativeOne.getText();
        assertTrue(text.contains("Concrete method"));
        assertTrue(text.contains("30 → 40"));
        assertFalse(text.contains("Named location"));
    }

    @Test
    public void fallbackHidesControlsThatCannotDoAnything()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        panel.updateRecommendations(Collections.emptyList());

        assertFalse(panel.detailsButton.isVisible());
        assertFalse(panel.feedbackPanel.isVisible());
        assertFalse(panel.progressBar.isVisible());
    }

    @Test
    public void opportunityCardExplainsFirstPreparationInsteadOfDebugConfidence()
    {
        OsrsStrategistPanel panel = panel("", value -> { }, () -> { });
        Opportunity herb = new Opportunity("opportunity:herb-run",
                OpportunityType.HERB_RUN, "Herb run", true,
                Confidence.VERIFIED,
                Collections.singletonList("Carry a spade"), false,
                Safety.skill(false,
                        net.runelite.api.Skill.FARMING));

        panel.updateOpportunities(Collections.singletonList(herb));

        assertTrue(panel.opportunityOne.getText()
                .contains("Prep: Carry a spade"));
        assertFalse(panel.opportunityOne.getText()
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
                Confidence.VERIFIED, 0, 0,
                new Guidance("Start the quest.", null,
                        "Quest start", null));
    }

    private static Recommendation recommendation(String id, String title)
    {
        net.runelite.api.Skill skill = id.contains("fishing")
                ? net.runelite.api.Skill.FISHING : net.runelite.api.Skill.MINING;
        TrainingMethod method = new TrainingMethod(id + ":method", skill,
                1, 99, "Concrete method", "Named location", 1, 1, 1,
                AttentionLevel.MODERATE, 10, 1, Collections.emptyList(),
                Confidence.VERIFIED);
        return new Recommendation(id, title, "Reason", 1,
                new TrainingPlan(method, "Reason",
                        Confidence.VERIFIED, Collections.emptyList()),
                Confidence.VERIFIED, 30, 40,
                new Guidance("Repeat the exact loop.",
                        "Required tool.", "Named location.", null));
    }
}
