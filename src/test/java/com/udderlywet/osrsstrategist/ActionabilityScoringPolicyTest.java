package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ActionabilityScoringPolicyTest
{
    private final ActionabilityScoringPolicy policy =
            new ActionabilityScoringPolicy();

    @Test
    public void verifiedActionGetsSmallBonus()
    {
        Recommendation ready = recommendation(
                "ready", 50.0, RecommendationConfidence.VERIFIED,
                Collections.emptyList());
        assertEquals(2.5, policy.adjustmentFor(ready), 0.0001);
    }

    @Test
    public void severalChecksCreateBoundedPenalty()
    {
        Recommendation check = recommendation(
                "check", 50.0, RecommendationConfidence.CHECK_NEEDED,
                Arrays.asList(
                        unresolved("one"), unresolved("two"),
                        unresolved("three"), unresolved("four"),
                        unresolved("five"), unresolved("six")));
        double adjustment = policy.adjustmentFor(check);
        assertTrue(adjustment < 0.0);
        assertTrue(adjustment >= -3.5);
    }

    @Test
    public void readinessCanResolveNearTie()
    {
        Recommendation check = recommendation(
                "check", 51.0, RecommendationConfidence.CHECK_NEEDED,
                Arrays.asList(unresolved("bank"), unresolved("access")));
        Recommendation ready = recommendation(
                "ready", 50.0, RecommendationConfidence.VERIFIED,
                Collections.emptyList());

        java.util.List<Recommendation> adjusted = policy.adjust(
                Arrays.asList(check, ready));
        double readyScore = score(adjusted, "ready");
        double checkScore = score(adjusted, "check");
        assertTrue(readyScore > checkScore);
    }

    @Test
    public void readinessCannotEraseLargeStrategicAdvantage()
    {
        Recommendation important = recommendation(
                "important", 65.0, RecommendationConfidence.CHECK_NEEDED,
                Arrays.asList(unresolved("access"), unresolved("supply"),
                        unresolved("transport")));
        Recommendation easy = recommendation(
                "easy", 50.0, RecommendationConfidence.VERIFIED,
                Collections.emptyList());

        java.util.List<Recommendation> adjusted = policy.adjust(
                Arrays.asList(important, easy));
        assertTrue(score(adjusted, "important") > score(adjusted, "easy"));
    }

    private static Recommendation recommendation(
            String id,
            double score,
            RecommendationConfidence confidence,
            java.util.List<RequirementCheck> checks)
    {
        TrainingMethod method = new TrainingMethod(
                id, net.runelite.api.Skill.COOKING,
                1, 99, id, "test", 10, 10, 10,
                AttentionLevel.MODERATE, 10, 1,
                Collections.emptyList(), confidence);
        TrainingPlan plan = new TrainingPlan(
                method, "test", confidence, checks);
        return new Recommendation(
                id, id, "test", score, plan, confidence, 1, 10);
    }

    private static RequirementCheck unresolved(String label)
    {
        return new RequirementCheck(
                "check:" + label, label,
                RequirementState.CHECK_NEEDED, "test");
    }

    private static double score(
            java.util.List<Recommendation> values,
            String id)
    {
        return values.stream()
                .filter(value -> id.equals(value.getId()))
                .findFirst().orElseThrow(AssertionError::new)
                .getScore();
    }
}
