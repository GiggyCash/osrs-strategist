package compass;

import java.util.Arrays;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Protects the compact-sidebar UX from accidentally becoming a wall of text
 * while also ensuring Needs Info always tells the player what is unresolved.
 */
public class RecommendationPresentationTest
{
    @Test
    public void compactViewHidesDeepExplanation()
    {
        Recommendation recommendation = recommendation();

        String compact = Presentation.compactHtml(
                recommendation
        );

        assertTrue(compact.contains("METHOD"));
        assertTrue(compact.contains("NEEDED"));
        assertTrue(compact.contains("Planks/materials"));
        assertFalse(compact.contains("WHY IT MATTERS"));
        assertFalse(compact.contains("HOW"));
        assertFalse(compact.contains("Current:"));
        assertFalse(compact.contains("Verified POH access"));
        assertTrue("Compact copy should stay short", compact.length() < 450);
    }

    @Test
    public void detailedViewKeepsOnlyDecisionCriticalSections()
    {
        String detailed = Presentation.detailedHtml(
                recommendation()
        );

        assertTrue(detailed.contains("WHY"));
        assertTrue(detailed.contains("NEEDED"));
        assertTrue(detailed.contains("CURRENT STEP"));
        assertTrue(detailed.contains("Planks/materials"));
        assertFalse(detailed.contains("Need to confirm materials"));
        assertFalse(detailed.contains("Verified POH access"));
        assertTrue(detailed.length() < 500);
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
                Confidence.CHECK_NEEDED
        );

        TrainingPlan plan = new TrainingPlan(
                method,
                "Selected for the current strategy style.",
                Confidence.CHECK_NEEDED,
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
                Confidence.CHECK_NEEDED,
                1,
                10
        );
    }
}
