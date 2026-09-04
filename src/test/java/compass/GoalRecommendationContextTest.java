package compass;

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
                        Confidence.VERIFIED), Membership.P2P);
        GoalRecommendationContext max = GoalRecommendationContext.assess(
                GoalType.MAX, recommendation("skill:woodcutting",
                        Confidence.VERIFIED), Membership.P2P);

        assertEquals(GoalRelation.AUTOMATIC,
                automatic.getRelationship());
        assertEquals("Automatic", automatic.getGoalName());
        assertEquals("Max cape", max.getGoalName());
        assertEquals(PlayerGoal.AUTOMATIC,
                new OsrsStrategistConfig() { }.activeGoal());
    }

    @Test
    public void bowfaRelationshipNeverPretendsGenericWorkIsDirect()
    {
        GoalRecommendationContext direct = GoalRecommendationContext.assess(
                GoalType.BOWFA, recommendation("upgrade:bowfa",
                        Confidence.VERIFIED).withGoalProvenance(
                                GoalProvenance.direct(GoalType.BOWFA,
                                        "upgrade:bowfa", java.util.Arrays.asList(
                                                "Bowfa", "Enhanced crystal weapon seed"))),
                Membership.P2P);
        GoalRecommendationContext fallback = GoalRecommendationContext.assess(
                GoalType.BOWFA, recommendation("skill:woodcutting",
                        Confidence.VERIFIED), Membership.P2P);

        assertEquals(GoalRelation.DIRECT,
                direct.getRelationship());
        assertEquals(GoalRelation.FALLBACK,
                fallback.getRelationship());
        assertTrue(fallback.getStatus().isEmpty());
    }

    @Test
    public void unavailableMembersGoalExplainsSafeFallback()
    {
        GoalRecommendationContext context = GoalRecommendationContext.assess(
                GoalType.BOWFA, recommendation("skill:woodcutting",
                        Confidence.VERIFIED), Membership.F2P);
        assertEquals(GoalRelation.FALLBACK,
                context.getRelationship());
        assertTrue(context.getStatus().contains("requires members content"));
        assertTrue(context.getStatus().contains("F2P progression"));
    }

    @Test
    public void unknownMembershipStaysCheckNeeded()
    {
        GoalRecommendationContext context = GoalRecommendationContext.assess(
                GoalType.PRIFDDINAS, recommendation("skill:agility",
                        Confidence.VERIFIED), Membership.UNKNOWN);
        assertEquals(GoalRelation.CHECK_NEEDED,
                context.getRelationship());
        assertTrue(context.getStatus().startsWith("Confirm membership"));
    }

    private static Recommendation recommendation(String id,
            Confidence confidence)
    {
        return new Recommendation(id, "A useful move", "Useful progression.",
                1.0, null, confidence, 0, 0,
                new Guidance("Do the current step.", null,
                        null, null));
    }
}
