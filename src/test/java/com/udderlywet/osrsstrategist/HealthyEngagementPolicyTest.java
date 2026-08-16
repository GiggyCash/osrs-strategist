package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Guards the healthy-engagement policy against becoming an opaque retention
 * mechanic. Repeated behavior may gently break ties, but one choice must never
 * redefine the player's preferences and important score gaps must remain intact.
 */
public class HealthyEngagementPolicyTest
{
    private static final long NOW = 10_000_000_000L;

    @Test
    public void oneSkipDoesNotPenalizeWholeFamily()
    {
        RecommendationHistory history = history(
                event("skill:mining", RecommendationHistoryAction.LATER, NOW - 60_000L));
        Recommendation mining = recommendation("skill:mining", 50.0);

        double adjustment = new HealthyEngagementPolicy().adjustmentFor(
                mining, history, VarietyPreference.BALANCED, NOW);

        assertEquals(0.0, adjustment, 0.0001);
    }

    @Test
    public void repeatedAvoidanceCreatesSmallTemporaryFamilyPenalty()
    {
        RecommendationHistory history = history(
                event("skill:mining", RecommendationHistoryAction.NOT_TODAY, NOW - 60_000L),
                event("skill:fishing", RecommendationHistoryAction.LATER, NOW - 120_000L));
        Recommendation woodcutting = recommendation("skill:woodcutting", 50.0);

        double adjustment = new HealthyEngagementPolicy().adjustmentFor(
                woodcutting, history, VarietyPreference.BALANCED, NOW);

        assertTrue(adjustment < 0.0);
        assertTrue(adjustment >= -5.0);
    }

    @Test
    public void threeRecentCompletionsEncourageFreshFamily()
    {
        RecommendationHistory history = history(
                event("skill:mining", RecommendationHistoryAction.COMPLETED, NOW - 60_000L),
                event("skill:fishing", RecommendationHistoryAction.COMPLETED, NOW - 120_000L),
                event("skill:woodcutting", RecommendationHistoryAction.COMPLETED, NOW - 180_000L));

        double gathering = new HealthyEngagementPolicy().adjustmentFor(
                recommendation("skill:hunter", 50.0), history,
                VarietyPreference.FRESH, NOW);
        double combat = new HealthyEngagementPolicy().adjustmentFor(
                recommendation("skill:attack", 50.0), history,
                VarietyPreference.FRESH, NOW);

        assertTrue(gathering < 0.0);
        assertEquals(0.0, combat, 0.0001);
    }

    @Test
    public void protectedProgressionIgnoresCompletionRepetitionPenalty()
    {
        RecommendationHistory history = history(
                event("skill:agility", RecommendationHistoryAction.COMPLETED, NOW - 60_000L),
                event("skill:agility", RecommendationHistoryAction.COMPLETED, NOW - 120_000L),
                event("skill:agility", RecommendationHistoryAction.COMPLETED, NOW - 180_000L));

        TrainingMethod protectedMethod = new TrainingMethod(
                "agility_graceful", net.runelite.api.Skill.AGILITY,
                1, 99, "Rooftops for Graceful", "Run rooftops.",
                10, 10, 10, AttentionLevel.MODERATE, 20, 2,
                java.util.Collections.emptyList(), RecommendationConfidence.VERIFIED,
                true, false, true);
        TrainingPlan plan = new TrainingPlan(
                protectedMethod, "Protected outfit progression.",
                RecommendationConfidence.VERIFIED,
                java.util.Collections.emptyList());
        Recommendation recommendation = new Recommendation(
                "skill:agility", "Continue Graceful", "Protected goal.",
                50.0, plan, RecommendationConfidence.VERIFIED, 50, 60);

        double adjustment = new HealthyEngagementPolicy().adjustmentFor(
                recommendation, history, VarietyPreference.FRESH, NOW);

        assertEquals(0.0, adjustment, 0.0001);
    }

    @Test
    public void familyAdjustmentCannotOverturnLargeStrategicGap()
    {
        RecommendationHistory history = history(
                event("skill:mining", RecommendationHistoryAction.DISLIKE, NOW - 60_000L),
                event("skill:fishing", RecommendationHistoryAction.NOT_TODAY, NOW - 120_000L),
                event("skill:woodcutting", RecommendationHistoryAction.LATER, NOW - 180_000L));

        HealthyEngagementPolicy policy = new HealthyEngagementPolicy();
        java.util.List<Recommendation> adjusted = policy.adjust(
                Arrays.asList(
                        recommendation("skill:mining", 70.0),
                        recommendation("quest:test", 55.0)),
                history,
                VarietyPreference.FRESH);

        Recommendation gathering = adjusted.stream()
                .filter(value -> "skill:mining".equals(value.getId()))
                .findFirst().orElseThrow(AssertionError::new);
        Recommendation quest = adjusted.stream()
                .filter(value -> "quest:test".equals(value.getId()))
                .findFirst().orElseThrow(AssertionError::new);

        assertTrue(gathering.getScore() > quest.getScore());
        assertTrue(70.0 - gathering.getScore() <= 5.0);
    }

    @Test
    public void freshModeIsStrongerThanFocusedButBothStayCapped()
    {
        RecommendationHistory history = history(
                event("skill:mining", RecommendationHistoryAction.DISLIKE, NOW - 60_000L),
                event("skill:fishing", RecommendationHistoryAction.NOT_TODAY, NOW - 120_000L));
        Recommendation gathering = recommendation("skill:woodcutting", 50.0);
        HealthyEngagementPolicy policy = new HealthyEngagementPolicy();

        double focused = policy.adjustmentFor(
                gathering, history, VarietyPreference.FOCUSED, NOW);
        double fresh = policy.adjustmentFor(
                gathering, history, VarietyPreference.FRESH, NOW);

        assertTrue(fresh < focused);
        assertTrue(fresh >= -5.0);
    }

    private static Recommendation recommendation(String id, double score)
    {
        return new Recommendation(id, id, "test", score);
    }

    private static RecommendationHistory history(RecommendationHistoryEntry... entries)
    {
        RecommendationHistory history = new RecommendationHistory();
        history.replaceAll(Arrays.asList(entries));
        return history;
    }

    private static RecommendationHistoryEntry event(
            String id,
            RecommendationHistoryAction action,
            long time)
    {
        return new RecommendationHistoryEntry(id, id, action, time);
    }
}
