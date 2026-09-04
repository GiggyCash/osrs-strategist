package compass;

import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression coverage for the production primary-sidebar presentation path. */
public class PrimarySidebarMethodPresentationTest
{
    private static final String METHOD = "Cook carried fish without banking";

    @Test
    public void resolvedMethodRemainsVisibleThroughLoginAndRefresh()
    {
        Recommendation recommendation = cookingRecommendation();
        GuidanceChecklist checklist = new MethodGuidanceService(
                TestFixtures.farmingRunPlanner(new FarmingRunCatalog()))
                .build(recommendation, null);

        assertFalse(checklist.getTitle().trim().isEmpty());
        assertEquals(METHOD, checklist.getTitle());

        OsrsStrategistPanel panel = panel();
        SidebarAccessibility.apply(panel, SidebarTextSize.STANDARD);
        panel.updateAccount("Live UIM", "Ultimate Ironman",
                Membership.F2P, 500);
        panel.updateGoal(GoalType.AUTOMATIC);
        panel.updateStrategy(StrategyMode.EFFICIENT,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL);
        panel.updateRecommendations(Collections.singletonList(recommendation));

        assertSidebarMatchesChecklist(panel, checklist);

        // The normal login/account initialization calls must not replace the
        // already resolved primary card with an empty-height rendering.
        panel.updateAccount("Live UIM", "Ultimate Ironman",
                Membership.F2P, 500);
        panel.updateGoal(GoalType.AUTOMATIC);
        assertSidebarMatchesChecklist(panel, checklist);

        // Recalculation commonly returns a fresh object with the same semantic
        // recommendation ID. Its method must be measured and painted again.
        Recommendation refreshed = cookingRecommendation();
        panel.updateRecommendations(Collections.singletonList(refreshed));
        GuidanceChecklist refreshedChecklist = new MethodGuidanceService(
                TestFixtures.farmingRunPlanner(new FarmingRunCatalog()))
                .build(refreshed, null);
        assertSidebarMatchesChecklist(panel, refreshedChecklist);
    }

    private static void assertSidebarMatchesChecklist(
            OsrsStrategistPanel panel, GuidanceChecklist checklist)
    {
        String sidebar = panel.recommendationBody.getText();
        assertTrue(sidebar.contains("METHOD\n" + checklist.getTitle()));
        assertTrue(sidebar.contains("BRING\nBring the carried raw fish"));
        assertTrue(sidebar.contains("WHERE\nBarbarian Village river / permanent fire"));
        assertTrue(sidebar.contains("DO\nCook the carried raw fish"));

        // Assert paintable geometry, not only the JTextArea backing string.
        // Before the fix this height was exactly one line, so RuneLite painted
        // METHOD and clipped its value plus every following primary field.
        assertTrue(panel.recommendationBody.getPreferredSize().height
                >= panel.recommendationBody.getLineCount()
                * panel.recommendationBody.getFontMetrics(panel.recommendationBody.getFont()).getHeight());
    }

    private static OsrsStrategistPanel panel()
    {
        return new OsrsStrategistPanel((id, feedback) -> { }, null,
                recommendation -> { }, () -> { }, () -> { }, "", value -> { });
    }

    private static Recommendation cookingRecommendation()
    {
        TrainingMethod method = new TrainingMethod(
                "cooking_f2p_uim_carried",
                Skill.COOKING,
                1,
                99,
                METHOD,
                "Cook the carried raw fish on the permanent fire.",
                10,
                10,
                10,
                AttentionLevel.LOW,
                10,
                0,
                Collections.emptyList(),
                Confidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(
                method,
                "Use the observed raw fish without creating a bank loop.",
                Confidence.VERIFIED,
                Collections.emptyList()).withCurrentStageTargetLevel(20);
        Guidance guidance = new Guidance(
                "Cook the carried raw fish",
                "Bring the carried raw fish",
                "Barbarian Village river / permanent fire",
                null,
                BankingMode.LOCAL_PROCESSING).withProgress("19 → 20");
        return new Recommendation(
                "skill:cooking",
                "Train Cooking to 20",
                "Finish the current Cooking checkpoint.",
                50,
                plan,
                Confidence.VERIFIED,
                19,
                20,
                guidance,
                Safety.skill(true, Skill.COOKING));
    }
}
