package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RecommendationPickerTest
{
    private final RecommendationPicker picker = new RecommendationPicker();

    @Test
    public void bestAlwaysReturnsHighestScore()
    {
        Recommendation best = rec("best", 80);
        Recommendation other = rec("other", 70);
        assertEquals("best", picker.pick(
                Arrays.asList(other, best),
                RecommendationSelectionMode.BEST, 1).getId());
    }

    @Test
    public void surpriseCannotReachFarBelowUsefulScoreWindow()
    {
        Recommendation best = rec("best", 80);
        Recommendation close = rec("close", 75);
        Recommendation bad = rec("bad", 40);
        for (int entropy = 0; entropy < 20; entropy++)
        {
            String id = picker.pick(Arrays.asList(best, close, bad),
                    RecommendationSelectionMode.SURPRISE, entropy).getId();
            if ("bad".equals(id)) throw new AssertionError("Bad option entered Surprise pool");
        }
    }

    private static Recommendation rec(String id, double score)
    {
        return new Recommendation(
                id, id, "reason", score, null,
                RecommendationConfidence.VERIFIED, 0, 0);
    }
}
