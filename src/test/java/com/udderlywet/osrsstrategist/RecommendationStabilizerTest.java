package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RecommendationStabilizerTest
{
    @Test
    public void minorRerankKeepsTheCurrentCheckpointSteady()
    {
        Recommendation oldTop = ready("skill:mining", "Train Mining to 50", 50);
        StrategyResult fresh = result(
                ready("skill:fishing", "Train Fishing to 50", 51),
                ready("skill:mining", "Train Mining to 50", 50));
        StrategyResult stable = new RecommendationStabilizer().stabilize(
                Collections.singletonList(oldTop), fresh);
        assertEquals("skill:mining", stable.getRecommendations().get(0).getId());
    }

    @Test
    public void incidentalXpInventoryBankAndUiRefreshesKeepCheckpointSteady()
    {
        String[] refreshes = {"XP tick", "inventory", "bank", "UI"};
        for (int i = 0; i < refreshes.length; i++)
        {
            Recommendation oldTop = ready(
                    "skill:mining", "Train Mining to 50", 50);
            Recommendation refreshed = ready(
                    "skill:mining", "Train Mining to 50", 49 - i);
            StrategyResult stable = new RecommendationStabilizer().stabilize(
                    Collections.singletonList(oldTop), result(
                            ready("skill:fishing", "Train Fishing to 50", 51),
                            refreshed));
            assertEquals(refreshes[i], "skill:mining",
                    stable.getRecommendations().get(0).getId());
            assertSame(refreshes[i], refreshed,
                    stable.getRecommendations().get(0));
        }
    }

    @Test
    public void completedCheckpointAdvancesInsteadOfBeingPinned()
    {
        Recommendation oldTop = ready("skill:mining", "Train Mining to 50", 50);
        StrategyResult fresh = result(
                ready("skill:fishing", "Train Fishing to 50", 51),
                ready("skill:mining", "Train Mining to 60", 50));
        StrategyResult stable = new RecommendationStabilizer().stabilize(
                Collections.singletonList(oldTop), fresh);
        assertEquals("skill:fishing", stable.getRecommendations().get(0).getId());
    }

    @Test
    public void dismissedRecommendationCannotBeResurrected()
    {
        Recommendation oldTop = ready("skill:mining", "Train Mining to 50", 50);
        StrategyResult fresh = result(
                ready("skill:fishing", "Train Fishing to 50", 51));
        StrategyResult stable = new RecommendationStabilizer().stabilize(
                Collections.singletonList(oldTop), fresh);
        assertEquals("skill:fishing", stable.getRecommendations().get(0).getId());
    }

    @Test
    public void materialInvalidationsCannotBeResurrected()
    {
        String[] causes = {"quest completed", "membership changed",
                "account changed", "method invalid", "Later", "Not Today",
                "Dislike", "required resource unavailable",
                "completion condition true"};
        for (String cause : causes)
        {
            Recommendation oldTop = ready(
                    "skill:mining", "Train Mining to 50", 50);
            StrategyResult stable = new RecommendationStabilizer().stabilize(
                    Collections.singletonList(oldTop), result(
                            ready("skill:fishing", "Train Fishing to 50", 51)));
            assertEquals(cause, "skill:fishing",
                    stable.getRecommendations().get(0).getId());
        }
    }

    @Test
    public void changedMethodGuidanceIsAppliedEvenWhenSkillStaysPinned()
    {
        Recommendation oldTop = skillPlan("skill:mining",
                "Train Mining to 50", 50, "mining_stars",
                "Mine the observed star.");
        Recommendation replacementMethod = skillPlan("skill:mining",
                "Train Mining to 50", 49, "mining_f2p_iron",
                "Mine iron and drop it when full.");

        StrategyResult stable = new RecommendationStabilizer().stabilize(
                Collections.singletonList(oldTop), result(
                        ready("skill:fishing", "Train Fishing to 50", 51),
                        replacementMethod));

        Recommendation top = stable.getRecommendations().get(0);
        assertSame(replacementMethod, top);
        assertEquals("mining_f2p_iron",
                top.getTrainingPlan().getMethod().getId());
        assertEquals("Mine iron and drop it when full.",
                top.getGuidance().getAction());
    }

    @Test
    public void resourceLossReplacesExecutionGuidanceImmediately()
    {
        Recommendation oldTop = skillPlan("skill:mining",
                "Train Mining to 50", 50, "mining_f2p_iron",
                "Mine iron and drop it when full.");
        Recommendation acquireTool = skillPlan("skill:mining",
                "Train Mining to 50", 49, "mining_f2p_iron",
                "Get a bronze pickaxe from the Mining tutor, then mine iron.");

        StrategyResult stable = new RecommendationStabilizer().stabilize(
                Collections.singletonList(oldTop), result(
                        ready("skill:fishing", "Train Fishing to 50", 51),
                        acquireTool));

        assertSame(acquireTool, stable.getRecommendations().get(0));
        assertTrue(stable.getRecommendations().get(0).getGuidance().getAction()
                .contains("Get a bronze pickaxe"));
    }

    @Test
    public void deliberateGoalAndAccountChangesBypassHysteresis()
    {
        for (String cause : new String[] {"goal changed", "account changed"})
        {
            StrategyResult changed = new RecommendationStabilizer().stabilize(
                    Collections.emptyList(), result(
                            ready("skill:fishing", "Train Fishing to 50", 40)));
            assertEquals(cause, "skill:fishing",
                    changed.getRecommendations().get(0).getId());
        }
    }

    @Test
    public void materiallyBetterNewPlanReplacesTheOldOne()
    {
        Recommendation oldTop = ready("skill:mining", "Train Mining to 50", 50);
        StrategyResult fresh = result(
                ready("opportunity:birdhouse", "Run birdhouses", 70),
                ready("skill:mining", "Train Mining to 50", 50));

        StrategyResult stable = new RecommendationStabilizer().stabilize(
                Collections.singletonList(oldTop), fresh);

        assertEquals("opportunity:birdhouse",
                stable.getRecommendations().get(0).getId());
    }

    private static StrategyResult result(Recommendation... recommendations)
    {
        return new StrategyResult(Arrays.asList(recommendations),
                Collections.emptyList());
    }

    private static Recommendation ready(String id, String title, double score)
    {
        return new Recommendation(id, title, "Reason.", score, null,
                Confidence.VERIFIED, 49, 50,
                new Guidance("Follow the named route.",
                        "Required setup.", "Named location.", null),
                SafetyEvidence.harmless(true));
    }

    private static Recommendation skillPlan(String id, String title,
            double score, String methodId, String action)
    {
        TrainingMethod method = new TrainingMethod(
                methodId, net.runelite.api.Skill.MINING, 1, 99,
                "Concrete mining route", "Varrock East mine.",
                10, 10, 10, AttentionLevel.MODERATE,
                20, 1, Collections.emptyList(),
                Confidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(method, "Concrete route.",
                Confidence.VERIFIED, Collections.emptyList());
        return new Recommendation(id, title, "Reason.", score, plan,
                Confidence.VERIFIED, 49, 50,
                new Guidance(action, "Bronze pickaxe.",
                        "Varrock East mine, southeast of Varrock.", null),
                SafetyEvidence.harmless(true));
    }
}
