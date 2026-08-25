package com.udderlywet.osrsstrategist;

import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MethodGuidanceReleaseTest
{
    @Test
    public void guidanceUsesHeadsUpFieldsInsteadOfPlannerParagraphs()
    {
        GuidanceChecklist checklist = service().build(recommendation(
                "A knife and the verified logs.",
                "The nearest bank or chopping area.",
                "Fletch the verified shortfall to level 60.",
                "This ordinary background explanation should not be shown."), null);

        assertEquals("Maple longbows", checklist.getTitle());
        assertTrue(checklist.getBring().contains("knife"));
        assertTrue(checklist.getWhere().contains("nearest"));
        assertTrue(checklist.getAction().contains("level 60"));
        assertEquals("Level 55 → 60", checklist.getProgress());
        assertNull(checklist.getImportant());
    }

    @Test
    public void safetyCriticalCaveatSurvivesCompaction()
    {
        GuidanceChecklist checklist = service().build(recommendation(
                "A knife and the verified logs.", "A safe bank.",
                "Prepare the method.",
                "Hardcore accounts must not use the Wilderness alternative."), null);
        assertTrue(checklist.getImportant().contains("Hardcore"));
        assertTrue(checklist.getImportant().contains("Wilderness"));
    }

    private static MethodGuidanceService service()
    {
        return new MethodGuidanceService(
                new FarmingRunPlanner(new FarmingRunCatalog()));
    }

    private static Recommendation recommendation(String supplies,
            String location, String action, String note)
    {
        TrainingMethod method = new TrainingMethod("fletching:maple",
                Skill.FLETCHING, 55, 99, "Maple longbows",
                "Fletch maple longbows.", 10, 10, 10,
                AttentionLevel.LOW, 10, 2, Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(method, "Suitable for the session.",
                RecommendationConfidence.VERIFIED, Collections.emptyList());
        return new Recommendation("skill:fletching", "Train Fletching to 60",
                "Unlocks the next method.", 20, plan,
                RecommendationConfidence.VERIFIED, 55, 60,
                new RecommendationGuidance(action, supplies, location, note));
    }
}
