package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Protects the compact-sidebar UX from accidentally becoming a wall of text.
 */
public class RecommendationPresentationTest
{
    @Test
    public void compactViewHidesDeepExplanation()
    {
        Recommendation recommendation = recommendation();

        String compact = RecommendationPresentation.compactHtml(
                recommendation
        );

        assertTrue(compact.contains("BEST METHOD"));
        assertTrue(compact.contains("PREP"));
        assertTrue(compact.contains("1 → 10"));
        assertFalse(compact.contains("WHY IT MATTERS"));
        assertFalse(compact.contains("HOW"));
    }

    @Test
    public void detailedViewExposesInstructionsAndReasoning()
    {
        String detailed = RecommendationPresentation.detailedHtml(
                recommendation()
        );

        assertTrue(detailed.contains("HOW"));
        assertTrue(detailed.contains("WHY IT MATTERS"));
        assertTrue(detailed.contains("FULL PREP"));
    }

    private static Recommendation recommendation()
    {
        TrainingMethod method = new TrainingMethod(
                "test-construction",
                Skill.CONSTRUCTION,
                1,
                99,
                "Practical furniture",
                "Build the best verified furniture route for the account.",
                10.0,
                10.0,
                10.0,
                AttentionLevel.MODERATE,
                10,
                2,
                Arrays.asList(
                        "POH access",
                        "Planks/materials",
                        "Transport"
                ),
                RecommendationConfidence.CHECK_NEEDED
        );

        TrainingPlan plan = new TrainingPlan(
                method,
                "Selected for the current strategy style."
        );

        return new Recommendation(
                "skill:construction",
                "Train Construction to 10",
                "Builds useful POH progression.",
                50.0,
                plan,
                RecommendationConfidence.CHECK_NEEDED,
                1,
                10
        );
    }
}
