package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
                Collections.emptyList(), Collections.emptyList());
    }

    private static Recommendation ready(String id, String title, double score)
    {
        return new Recommendation(id, title, "Reason.", score, null,
                RecommendationConfidence.VERIFIED, 49, 50,
                new RecommendationGuidance("Follow the named route.",
                        "Required setup.", "Named location.", null),
                CandidateSafetyEvidence.harmless(true));
    }
}
