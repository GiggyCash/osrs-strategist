package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RecommendationHistoryTest
{
    @Test
    public void historyIsBoundedAndKeepsNewestEvents()
    {
        RecommendationHistory history = new RecommendationHistory();
        for (int i = 0; i < 250; i++)
        {
            history.add("skill:test:" + i, "Test", RecommendationHistoryAction.LATER);
        }
        assertEquals(200, history.snapshot().size());
        assertEquals("skill:test:50",
                history.snapshot().get(0).getActivityId());
    }
}
