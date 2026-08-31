package compass;

import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecommendationPresentationCompactTest
{
    @Test
    public void sidebarUsesRequestedMethodAndNeededHierarchy()
    {
        Recommendation recommendation = recommendationWithMethodAndMissingRequirement();
        String text = Presentation.compactText(recommendation);
        assertTrue(text.contains("METHOD"));
        assertTrue(text.contains("NEEDED"));
        assertFalse(text.contains("NEXT UNLOCK"));
        assertFalse(text.contains("BEST METHOD"));
        assertFalse(text.contains("NEEDS INFO"));
    }

    @Test
    public void nonSkillCardsUseActivityHierarchyAndNeverExposeDebugCopy()
    {
        Recommendation recommendation = new Recommendation(
                "stash:master:very-long", "Build the current Master STASH unit",
                "Avoid repeated clue inventory setup after the unit is observed built.",
                40, null, Confidence.CHECK_NEEDED, 0, 0,
                new Guidance(
                        "Open the Construction interface at the exact STASH location and verify built state.",
                        "Bring the verified flatpack materials only after the live built-state check.",
                        "The active clue location.",
                        "Built state remains unknown."));
        String compact = Presentation.compactText(recommendation);
        assertTrue(compact.contains("ACTIVITY"));
        assertTrue(compact.contains("BRING"));
        assertTrue(compact.contains("WHERE"));
        assertTrue(compact.contains("DO"));
        assertFalse(compact.contains("NEXT UNLOCK"));
        assertFalse(compact.contains("Strategist will verify"));
        assertFalse(compact.contains("policy class"));
    }
    @Test
    public void compactCardDoesNotDumpFullPlannerParagraphs()
    {
        String longAction = "Cook 1,234 sharks at the range after withdrawing them in batches. "
                + "This second sentence contains intentionally detailed execution notes that belong in Details instead of the narrow sidebar.";
        String longSupplies = "Verified: you own 900 raw sharks in the bank, 100 in inventory, and need 234 more. "
                + "Buy the remaining fish and keep several other detailed accounting notes in the full overlay.";
        Recommendation recommendation = recommendation(longAction, longSupplies);

        String compact = Presentation.compactText(recommendation);
        String details = Presentation.detailedText(recommendation);

        assertTrue(compact.contains("Verified: you own 900 raw sharks"));
        assertFalse(compact.contains("intentionally detailed execution notes"));
        assertFalse(compact.contains("several other detailed accounting notes"));
        assertFalse(details.contains("intentionally detailed execution notes"));
        assertFalse(details.contains("several other detailed accounting notes"));
        assertTrue(details.contains("CURRENT STEP"));
        assertTrue(details.length() < 500);
    }

    @Test
    public void verifiedDetailsDoNotMislabelCarriedSetupAsMissing()
    {
        Recommendation verified = recommendation(
                "Cook the carried fish and bank the output.",
                "Carry the observed raw salmon.");
        String details = Presentation.detailedText(verified);

        assertTrue(details.contains("WHERE"));
        assertFalse(details.contains("NEEDED"));
        assertTrue(details.contains("CURRENT STEP"));
    }

    @Test
    public void compactSentenceNeverCutsIntoHugeWordWallBeyondBudget()
    {
        String value = "This is useful compact text followed by a very long explanation that should be shortened without destroying the full details presentation.";
        String compact = Presentation.compactSentence(value, 60);
        assertTrue(compact.length() <= 61);
        assertTrue(compact.endsWith("…") || compact.endsWith("."));
    }

    @Test
    public void longPlayerFacingContentStaysCompactAndKeepsHierarchy()
    {
        String longName = "Complete The Fremennik Isles using the verified Neitiznot travel and equipment preparation route";
        Recommendation recommendation = recommendation(
                longName + ". Bring the required quest items and follow the marked route.",
                "A very long required item name, food, teleport, and quest prerequisite list that must wrap inside the RuneLite sidebar.");

        String compact = Presentation.compactText(recommendation);
        assertTrue(compact.contains("METHOD"));
        assertTrue(compact.contains("BRING"));
        assertTrue(compact.contains("WHERE"));
        assertTrue(compact.contains("DO"));
        assertFalse(compact.contains("Compass will verify"));
        assertFalse(compact.contains("policy class"));
    }

    private static Recommendation recommendation(String action, String supplies)
    {
        TrainingMethod method = new TrainingMethod(
                "cooking_test", Skill.COOKING, 1, 99,
                "Cook banked food", "Cook the selected food.",
                10, 10, 10, AttentionLevel.MODERATE,
                20, 2, Collections.emptyList(),
                Confidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(
                method, "test", Confidence.VERIFIED,
                Collections.emptyList());
        return new Recommendation(
                "skill:cooking", "Train Cooking to 80", "Useful progression.",
                50.0, plan, Confidence.VERIFIED,
                70, 80,
                new Guidance(
                        action, supplies,
                        "Use the best verified range.",
                        "Detailed note that belongs in Details."));
    }

    private static Recommendation recommendationWithMethodAndMissingRequirement()
    {
        TrainingMethod method = new TrainingMethod(
                "cooking_test", Skill.COOKING, 1, 99,
                "Cook banked food", "Cook the selected food.",
                10, 10, 10, AttentionLevel.MODERATE,
                20, 2, Collections.emptyList(),
                Confidence.CHECK_NEEDED);
        RequirementCheck check = new RequirementCheck(
                "access:range", "Verify range access",
                RequirementState.CHECK_NEEDED, "No range access observation yet.");
        TrainingPlan plan = new TrainingPlan(method, "test",
                Confidence.CHECK_NEEDED,
                Collections.singletonList(check));
        return new Recommendation(
                "skill:cooking", "Train Cooking", "Useful progression.",
                50.0, plan, Confidence.CHECK_NEEDED,
                70, 80, null);
    }
}
