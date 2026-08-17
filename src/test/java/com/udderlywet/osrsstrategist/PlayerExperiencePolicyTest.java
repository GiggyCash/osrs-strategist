package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlayerExperiencePolicyTest
{
    @Test
    public void recentCompletionSoftlyFavorsAnotherUsefulOption()
    {
        RecommendationHistory history = new RecommendationHistory();
        history.add("skill:runecraft", "Train Runecraft", RecommendationHistoryAction.COMPLETED);

        Recommendation repeated = new Recommendation(
                "skill:runecraft", "Train Runecraft again", "", 50.0);
        Recommendation alternative = new Recommendation(
                "quest:dragon-slayer", "Dragon Slayer", "", 47.0);

        StrategyResult input = new StrategyResult(
                Arrays.asList(repeated, alternative),
                Collections.emptyList(),
                Collections.emptyList());

        StrategyResult output = new PlayerExperiencePolicy().rerank(input, history);

        assertEquals("quest:dragon-slayer",
                output.getRecommendations().get(0).getId());
        assertTrue(output.getRecommendations().get(1).getScore()
                < repeated.getScore());
    }

    @Test
    public void policyNeverHardBlocksStrategicallyImportantRecommendation()
    {
        RecommendationHistory history = new RecommendationHistory();
        for (int i = 0; i < 20; i++)
        {
            history.add("skill:agility", "Agility", RecommendationHistoryAction.COMPLETED);
        }

        Recommendation important = new Recommendation(
                "skill:agility", "Train Agility", "", 100.0);
        Recommendation weakAlternative = new Recommendation(
                "minigame:casual", "Casual minigame", "", 20.0);

        StrategyResult output = new PlayerExperiencePolicy().rerank(
                new StrategyResult(
                        Arrays.asList(important, weakAlternative),
                        Collections.emptyList(),
                        Collections.emptyList()),
                history);

        assertEquals("skill:agility",
                output.getRecommendations().get(0).getId());
        assertTrue(output.getRecommendations().get(0).getScore() >= 82.0);
    }

    @Test
    public void familyDetectionSupportsAllCurrentCandidateFamilies()
    {
        assertEquals("skill", PlayerExperiencePolicy.familyOf("skill:mining"));
        assertEquals("quest", PlayerExperiencePolicy.familyOf("quest:recipe-for-disaster"));
        assertEquals("pvm", PlayerExperiencePolicy.familyOf("pvm:barrows"));
        assertEquals("clue", PlayerExperiencePolicy.familyOf("clue:pending"));
        assertEquals("minigame", PlayerExperiencePolicy.familyOf("minigame:tempoross"));
    }
}
