package com.udderlywet.osrsstrategist;

import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecommendationPresentationCompactTest
{
    @Test
    public void compactCardDoesNotDumpFullPlannerParagraphs()
    {
        String longAction = "Cook 1,234 sharks at the range after withdrawing them in batches. "
                + "This second sentence contains intentionally detailed execution notes that belong in Details instead of the narrow sidebar.";
        String longSupplies = "Verified: you own 900 raw sharks in the bank, 100 in inventory, and need 234 more. "
                + "Buy the remaining fish and keep several other detailed accounting notes in the full overlay.";
        Recommendation recommendation = recommendation(longAction, longSupplies);

        String compact = RecommendationPresentation.compactText(recommendation);
        String details = RecommendationPresentation.detailedText(recommendation);

        assertTrue(compact.contains("Cook 1,234 sharks"));
        assertFalse(compact.contains("intentionally detailed execution notes"));
        assertFalse(compact.contains("several other detailed accounting notes"));
        assertTrue(details.contains("intentionally detailed execution notes"));
        assertTrue(details.contains("several other detailed accounting notes"));
    }

    @Test
    public void compactSentenceNeverCutsIntoHugeWordWallBeyondBudget()
    {
        String value = "This is useful compact text followed by a very long explanation that should be shortened without destroying the full details presentation.";
        String compact = RecommendationPresentation.compactSentence(value, 60);
        assertTrue(compact.length() <= 61);
        assertTrue(compact.endsWith("…") || compact.endsWith("."));
    }

    private static Recommendation recommendation(String action, String supplies)
    {
        TrainingMethod method = new TrainingMethod(
                "cooking_test", Skill.COOKING, 1, 99,
                "Cook banked food", "Cook the selected food.",
                10, 10, 10, AttentionLevel.MODERATE,
                20, 2, Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(
                method, "test", RecommendationConfidence.VERIFIED,
                Collections.emptyList());
        return new Recommendation(
                "skill:cooking", "Train Cooking to 80", "Useful progression.",
                50.0, plan, RecommendationConfidence.VERIFIED,
                70, 80,
                new RecommendationGuidance(
                        action, supplies,
                        "Use the best verified range.",
                        "Detailed note that belongs in Details."));
    }
}
