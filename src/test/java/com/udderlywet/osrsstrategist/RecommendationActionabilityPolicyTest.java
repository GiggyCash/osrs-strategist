package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecommendationActionabilityPolicyTest
{
    private final RecommendationActionabilityPolicy policy =
            new RecommendationActionabilityPolicy();

    @Test
    public void checkNeededCandidateCannotLeadQueue()
    {
        Recommendation candidate = new Recommendation(
                "quest:pandemonium",
                "Quest: Pandemonium",
                "Requirements still unknown",
                999.0,
                null,
                RecommendationConfidence.CHECK_NEEDED,
                0,
                0,
                null);

        assertFalse(policy.canLeadQueue(candidate));
        assertTrue(policy.mayAppearAsAlternative(candidate));
    }

    @Test
    public void verifiedStructuredActionCanLeadQueue()
    {
        Recommendation candidate = new Recommendation(
                "gear:test",
                "Get useful item",
                "Useful upgrade",
                20.0,
                null,
                RecommendationConfidence.VERIFIED,
                0,
                0,
                new RecommendationGuidance(
                        "Go get the item.",
                        "Bring food.",
                        "Test area",
                        "Safe test"));

        assertTrue(policy.canLeadQueue(candidate));
    }

    @Test
    public void verifiedTrainingWithUnresolvedCheckCannotLead()
    {
        TrainingMethod method = new TrainingMethod(
                "test",
                net.runelite.api.Skill.COOKING,
                1,
                99,
                "Test",
                "Cook food.",
                10,
                10,
                10,
                AttentionLevel.LOW,
                10,
                1,
                java.util.Collections.singletonList("bank"),
                RecommendationConfidence.CHECK_NEEDED);
        TrainingPlan plan = new TrainingPlan(
                method,
                "test",
                RecommendationConfidence.CHECK_NEEDED,
                java.util.Collections.singletonList(new RequirementCheck(
                        "bank",
                        "Open bank",
                        RequirementState.CHECK_NEEDED,
                        "Not observed")));
        Recommendation candidate = new Recommendation(
                "skill:cooking",
                "Train Cooking",
                "test",
                50,
                plan,
                RecommendationConfidence.VERIFIED,
                1,
                10,
                null);

        assertFalse(policy.canLeadQueue(candidate));
    }
}
