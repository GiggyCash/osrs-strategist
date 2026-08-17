package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Protects the compact-sidebar UX from accidentally becoming a wall of text
 * while still naming every unresolved prerequisite the player needs to check.
 */
public class RecommendationPresentationTest
{
    @Test
    public void compactViewHidesDeepExplanationAndInternalScoringLabels()
    {
        Recommendation recommendation = recommendation();

        String compact = RecommendationPresentation.compactHtml(
                recommendation
        );

        assertTrue(compact.contains("BEST METHOD"));
        assertTrue(compact.contains("CHECK BEFORE STARTING"));
        assertTrue(compact.contains("Planks/materials"));
        assertFalse(compact.contains("WHY IT MATTERS"));
        assertFalse(compact.contains("HOW"));
        assertFalse(compact.contains("Current:"));
        assertFalse(compact.contains("Verified POH access"));
        assertFalse(compact.contains("Moderate attention"));
        assertFalse(compact.contains("Needs Info"));
        assertTrue("Compact copy should stay short", compact.length() < 450);
    }

    @Test
    public void detailedViewExposesInstructionsReasoningAndEvidenceWithoutQuestionMarkers()
    {
        String detailed = RecommendationPresentation.detailedHtml(
                recommendation()
        );

        assertTrue(detailed.contains("HOW"));
        assertTrue(detailed.contains("WHY IT MATTERS"));
        assertTrue(detailed.contains("READINESS"));
        assertTrue(detailed.contains("POH access"));
        assertTrue(detailed.contains("Planks/materials"));
        assertTrue(detailed.contains("Need to confirm materials"));
        assertFalse(detailed.contains("? Planks/materials"));
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
                "Selected for the current strategy style.",
                RecommendationConfidence.CHECK_NEEDED,
                Arrays.asList(
                        new RequirementCheck(
                                "poh",
                                "POH access",
                                RequirementState.VERIFIED,
                                "Verified POH access"
                        ),
                        new RequirementCheck(
                                "planks",
                                "Planks/materials",
                                RequirementState.CHECK_NEEDED,
                                "Need to confirm materials"
                        ),
                        new RequirementCheck(
                                "transport",
                                "Transport",
                                RequirementState.CHECK_NEEDED,
                                "Need to confirm transport"
                        )
                )
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
