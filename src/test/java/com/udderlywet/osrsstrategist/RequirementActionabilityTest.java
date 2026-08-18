package com.udderlywet.osrsstrategist;

import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RequirementActionabilityTest
{
    @Test
    public void knownSupplyShortfallCanStillBeDoNext()
    {
        TrainingMethod method = method("smithing_test", Skill.SMITHING);
        TrainingPlan plan = new TrainingPlan(
                method,
                "test",
                RecommendationConfidence.CHECK_NEEDED,
                Collections.singletonList(new RequirementCheck(
                        "generic:Steel bar supply",
                        "Steel bar supply",
                        RequirementState.CHECK_NEEDED,
                        "Acquire the missing bars.")));
        Recommendation recommendation = new Recommendation(
                "skill:smithing",
                "Train Smithing to 50",
                "test",
                10.0,
                plan,
                RecommendationConfidence.CHECK_NEEDED,
                40,
                50,
                new RecommendationGuidance(
                        "Smith steel platebodies until the milestone.",
                        "Need 500 steel bars. Buy or self-source the missing amount.",
                        "Use a reachable anvil.",
                        "The route is known; only supplies remain."));

        RecommendationActionabilityPolicy policy =
                new RecommendationActionabilityPolicy();
        assertTrue(RequirementActionability.isActionablePreparation(
                plan, recommendation.getGuidance()));
        assertTrue(policy.canLeadQueue(recommendation));
        assertTrue(RecommendationPresentation.compactText(recommendation)
                .contains("Ready to prep"));
        assertTrue(RecommendationPresentation.compactText(recommendation)
                .contains("NEEDED"));
    }

    @Test
    public void unknownAccessCanNeverBeDoNext()
    {
        TrainingMethod method = method("minigame_test", Skill.MINING);
        TrainingPlan plan = new TrainingPlan(
                method,
                "test",
                RecommendationConfidence.CHECK_NEEDED,
                Collections.singletonList(new RequirementCheck(
                        "generic:Volcanic Mine access",
                        "Volcanic Mine access",
                        RequirementState.CHECK_NEEDED,
                        "Access has not been proven.")));
        Recommendation recommendation = new Recommendation(
                "skill:mining",
                "Train Mining",
                "test",
                1000.0,
                plan,
                RecommendationConfidence.CHECK_NEEDED,
                70,
                80,
                new RecommendationGuidance(
                        "Run Volcanic Mine.",
                        "Bring your best pickaxe.",
                        "Volcanic Mine.",
                        "Access must be verified."));

        assertFalse(RequirementActionability.isActionablePreparation(
                plan, recommendation.getGuidance()));
        assertTrue(RequirementActionability.hasHardUnresolvedRequirement(plan));
        assertFalse(new RecommendationActionabilityPolicy()
                .canLeadQueue(recommendation));
        assertTrue(RecommendationPresentation.compactText(recommendation)
                .contains("NEEDED"));
    }

    @Test
    public void blockedRequirementCanNeverBePrep()
    {
        TrainingMethod method = method("blocked_test", Skill.PRAYER);
        TrainingPlan plan = new TrainingPlan(
                method,
                "test",
                RecommendationConfidence.BLOCKED,
                Collections.singletonList(new RequirementCheck(
                        "build:restriction",
                        "Build restriction",
                        RequirementState.BLOCKED,
                        "This action would violate the protected account build.")));
        Recommendation recommendation = new Recommendation(
                "skill:prayer",
                "Train Prayer",
                "test",
                1000.0,
                plan,
                RecommendationConfidence.BLOCKED,
                1,
                43,
                new RecommendationGuidance(
                        "Do the blocked action.",
                        "None.",
                        "Nowhere.",
                        "Blocked."));

        assertFalse(new RecommendationActionabilityPolicy()
                .canLeadQueue(recommendation));
    }

    private static TrainingMethod method(String id, Skill skill)
    {
        return new TrainingMethod(
                id,
                skill,
                1,
                99,
                id,
                "test",
                10,
                10,
                10,
                AttentionLevel.MODERATE,
                10,
                1,
                Collections.emptyList(),
                RecommendationConfidence.CHECK_NEEDED);
    }
}
