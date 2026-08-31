package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

import lombok.Getter;

/** Short, player-facing explanation of a recommendation's goal relationship. */
@Getter
public final class GoalRecommendationContext
{
    private final GoalType goal;
    private final GoalRecommendationRelationship relationship;
    private final String status;
    private final GoalProvenance provenance;

    private GoalRecommendationContext(GoalType goal,
            GoalRecommendationRelationship relationship, String status,
            GoalProvenance provenance)
    {
        this.goal = goal == null ? GoalType.AUTOMATIC : goal;
        this.relationship = relationship == null
                ? GoalRecommendationRelationship.AUTOMATIC : relationship;
        this.status = status == null ? "" : status;
        this.provenance = provenance;
    }

    public static GoalRecommendationContext assess(GoalType goal,
            Recommendation recommendation, MembershipStatus membership)
    {
        var safeGoal = goal == null ? GoalType.AUTOMATIC : goal;
        if (safeGoal == GoalType.AUTOMATIC || safeGoal == GoalType.CUSTOM)
            return new GoalRecommendationContext(safeGoal,
                    GoalRecommendationRelationship.AUTOMATIC,
                    get(296), null);

        var name = displayName(safeGoal);
        if (requiresMembers(safeGoal) && membership != MembershipStatus.P2P)
            return new GoalRecommendationContext(safeGoal,
                    membership == MembershipStatus.UNKNOWN
                            ? GoalRecommendationRelationship.CHECK_NEEDED
                            : GoalRecommendationRelationship.FALLBACK,
                    membership == MembershipStatus.UNKNOWN
                            ? get(1229) + name + "."
                            : name + get(297),
                    null);

        if (recommendation == null)
            return new GoalRecommendationContext(safeGoal,
                    GoalRecommendationRelationship.CHECK_NEEDED,
                    get(298) + name + ".",
                    null);
        if (recommendation.getConfidence() == Confidence.BLOCKED)
            return new GoalRecommendationContext(safeGoal,
                    GoalRecommendationRelationship.BLOCKED,
                    get(1230) + name + " yet.",
                    null);

        var provenance = recommendation.getGoalProvenance();
        if (provenance != null
                && provenance.proves(safeGoal, recommendation.getId()))
        {
            if (recommendation.getConfidence()
                    == Confidence.CHECK_NEEDED)
                return new GoalRecommendationContext(safeGoal,
                        GoalRecommendationRelationship.CHECK_NEEDED,
                        get(1231) + name + ".",
                        provenance);
            boolean direct = provenance.getRelationship()
                    == GoalRecommendationRelationship.DIRECT;
            return new GoalRecommendationContext(safeGoal,
                    provenance.getRelationship(),
                    direct ? get(1232) + name + "."
                            : get(1233) + name + ".",
                    provenance);
        }
        return new GoalRecommendationContext(safeGoal,
                GoalRecommendationRelationship.FALLBACK,
                "", null);
    }

    public boolean hasProvenRelationship() { return provenance != null; }
    public boolean isAutomatic()
    {
        return relationship == GoalRecommendationRelationship.AUTOMATIC;
    }

    public String getGoalName() { return displayName(goal); }

    static String displayName(GoalType goal)
    {
        if (goal == null) return "Automatic";
        return goal.toString();
    }

    private static boolean requiresMembers(GoalType goal)
    {
        switch (goal)
        {
            case AUTOMATIC:
            case CUSTOM:
                return false;
            default:
                return true;
        }
    }
}
