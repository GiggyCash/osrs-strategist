package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GoalRecommendationContextTest
{
    @Test
    public void automaticIsDistinctFromTheMaxGoal()
    {
        GoalRecommendationContext automatic = GoalRecommendationContext.assess(
                GoalType.AUTOMATIC, recommendation("skill:woodcutting",
                        RecommendationConfidence.VERIFIED), MembershipStatus.P2P);
        GoalRecommendationContext max = GoalRecommendationContext.assess(
                GoalType.MAX, recommendation("skill:woodcutting",
                        RecommendationConfidence.VERIFIED), MembershipStatus.P2P);

        assertEquals(GoalRecommendationRelationship.AUTOMATIC,
                automatic.getRelationship());
        assertEquals("Automatic", automatic.getGoalName());
        assertEquals("Max", max.getGoalName());
        assertEquals(GoalType.AUTOMATIC,
                new OsrsStrategistConfig() { }.activeGoal());
    }

    @Test
    public void bowfaRelationshipNeverPretendsGenericWorkIsDirect()
    {
        GoalRecommendationContext direct = GoalRecommendationContext.assess(
                GoalType.BOWFA, recommendation("upgrade:bowfa",
                        RecommendationConfidence.VERIFIED), MembershipStatus.P2P);
        GoalRecommendationContext fallback = GoalRecommendationContext.assess(
                GoalType.BOWFA, recommendation("skill:woodcutting",
                        RecommendationConfidence.VERIFIED), MembershipStatus.P2P);

        assertEquals(GoalRecommendationRelationship.DIRECT,
                direct.getRelationship());
        assertEquals(GoalRecommendationRelationship.FALLBACK,
                fallback.getRelationship());
        assertTrue(fallback.getStatus().contains("no safe Bowfa step"));
    }

    @Test
    public void unavailableMembersGoalExplainsSafeFallback()
    {
        GoalRecommendationContext context = GoalRecommendationContext.assess(
                GoalType.BOWFA, recommendation("skill:woodcutting",
                        RecommendationConfidence.VERIFIED), MembershipStatus.F2P);
        assertEquals(GoalRecommendationRelationship.FALLBACK,
                context.getRelationship());
        assertTrue(context.getStatus().contains("requires members content"));
        assertTrue(context.getStatus().contains("F2P progression"));
    }

    @Test
    public void unknownMembershipStaysCheckNeeded()
    {
        GoalRecommendationContext context = GoalRecommendationContext.assess(
                GoalType.PRIFDDINAS, recommendation("skill:agility",
                        RecommendationConfidence.VERIFIED), MembershipStatus.UNKNOWN);
        assertEquals(GoalRecommendationRelationship.CHECK_NEEDED,
                context.getRelationship());
        assertTrue(context.getStatus().startsWith("Confirm membership"));
    }

    private static Recommendation recommendation(String id,
            RecommendationConfidence confidence)
    {
        return new Recommendation(id, "A useful move", "Useful progression.",
                1.0, null, confidence, 0, 0,
                new RecommendationGuidance("Do the current step.", null,
                        null, null));
    }
}
