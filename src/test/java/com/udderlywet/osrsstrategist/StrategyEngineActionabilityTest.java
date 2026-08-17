package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StrategyEngineActionabilityTest
{
    @Test
    public void highScoreNeedsInfoCannotBeatReadyAction()
    {
        Recommendation ready = new Recommendation(
                "skill:defence",
                "Train Defence to 80",
                "Protected build progression",
                40.0,
                null,
                RecommendationConfidence.VERIFIED,
                75,
                80,
                new RecommendationGuidance(
                        "Train Defence using a legal defensive style.",
                        "Use your current best legal gear.",
                        "Safe combat target",
                        "Preserve restricted combat stats."));
        Recommendation unresolved = new Recommendation(
                "quest:pandemonium",
                "Quest: Pandemonium",
                "Unknown requirements",
                500.0,
                null,
                RecommendationConfidence.CHECK_NEEDED,
                0,
                0,
                null);

        StrategyEngine engine = new StrategyEngine(
                null,
                null,
                null,
                null,
                new RecommendationActionabilityPolicy());
        List<Recommendation> queue = engine.buildPlayerQueue(
                Arrays.asList(unresolved, ready));

        assertEquals(2, queue.size());
        assertEquals("skill:defence", queue.get(0).getId());
        assertEquals("quest:pandemonium", queue.get(1).getId());
    }

    @Test
    public void noReadyActionProducesNoPrimaryRecommendation()
    {
        Recommendation unresolved = new Recommendation(
                "quest:test",
                "Quest: Test",
                "Unknown requirements",
                500.0,
                null,
                RecommendationConfidence.CHECK_NEEDED,
                0,
                0,
                null);
        StrategyEngine engine = new StrategyEngine(
                null,
                null,
                null,
                null,
                new RecommendationActionabilityPolicy());

        assertEquals(0, engine.buildPlayerQueue(
                java.util.Collections.singletonList(unresolved)).size());
    }
}
