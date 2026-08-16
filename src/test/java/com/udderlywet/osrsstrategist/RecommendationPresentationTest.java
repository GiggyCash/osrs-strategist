package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Protects the compact-sidebar UX from becoming a wall of text while ensuring
 * unresolved evidence is still explicit. Session intensity and deeper strategy
 * reasoning belong in Details, not in the glanceable recommendation header.
 */
public class RecommendationPresentationTest
{
    @Test
    public void compactViewShowsOnlyDecisionCriticalInformation()
    {
        String compact = RecommendationPresentation.compactHtml(
                recommendation());

        assertTrue(compact.contains("BEST METHOD"));
        assertTrue(compact.contains("CHECK FIRST"));
        assertTrue(compact.contains("Planks/materials"));
        assertTrue(compact.contains("Transport"));
        assertTrue(compact.contains("○"));
        assertFalse(compact.contains("NEEDS INFO"));
        assertFalse(compact.contains("Moderate attention"));
        assertFalse(compact.contains("WHY IT MATTERS"));
        assertFalse(compact.contains("WHY THIS METHOD"));
        assertFalse(compact.contains("HOW"));
        assertFalse(compact.contains("Current:"));
        assertFalse(compact.contains("Verified POH access"));
        assertFalse(compact.contains("?"));
        assertTrue("Compact copy should stay short", compact.length() < 450);
    }

    @Test
    public void detailedViewExposesInstructionsSessionFitReasoningAndEvidence()
    {
        String detailed = RecommendationPresentation.detailedHtml(
                recommendation());

        assertTrue(detailed.contains("HOW"));
        assertTrue(detailed.contains("SESSION FIT"));
        assertTrue(detailed.contains("Moderate attention"));
        assertTrue(detailed.contains("WHY THIS METHOD"));
        assertTrue(detailed.contains("WHY IT MATTERS"));
        assertTrue(detailed.contains("READINESS"));
        assertTrue(detailed.contains("POH access"));
        assertTrue(detailed.contains("Planks/materials"));
        assertTrue(detailed.contains("Need to confirm materials"));
        assertTrue(detailed.contains("○"));
        assertFalse(detailed.contains("?"));
    }

    @Test
    public void fullyVerifiedMethodGetsSimpleReadySignal()
    {
        TrainingMethod method = new TrainingMethod(
                "ready-method",
                Skill.WOODCUTTING,
                1,
                99,
                "Cut nearby trees",
                "Cut the best verified nearby tree for the session.",
                10.0,
                10.0,
                10.0,
                AttentionLevel.LOW,
                10,
                1,
                Arrays.asList("Axe"),
                RecommendationConfidence.VERIFIED
        );
        TrainingPlan plan = new TrainingPlan(
                method,
                "Fits a relaxed short session.",
                RecommendationConfidence.VERIFIED,
                Arrays.asList(new RequirementCheck(
                        "axe", "Axe", RequirementState.VERIFIED,
                        "Observed in inventory."))
        );
        Recommendation recommendation = new Recommendation(
                "skill:woodcutting", "Train Woodcutting to 10",
                "Useful early gathering progress.", 50.0, plan,
                RecommendationConfidence.VERIFIED, 1, 10);

        String compact = RecommendationPresentation.compactHtml(recommendation);
        assertTrue(compact.contains("✓ READY"));
        assertFalse(compact.contains("CHECK FIRST"));
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
