package compass;
import lombok.*;
import static compass.Text.get;


/** Short, player-facing explanation of a recommendation's goal relationship. */
@Getter
public final class GoalRecommendationContext
{
    final GoalType goal;
    final GoalRelation relationship;
    final String status;
    final GoalProvenance provenance;

    GoalRecommendationContext(GoalType goal,
            GoalRelation relationship, String status,
            GoalProvenance provenance)
    {
        this.goal = goal == null ? GoalType.AUTOMATIC : goal;
        this.relationship = relationship == null
                ? GoalRelation.AUTOMATIC : relationship;
        this.status = status == null ? "" : status;
        this.provenance = provenance;
    }

    public static GoalRecommendationContext assess(GoalType goal,
            Recommendation recommendation, Membership membership)
    {
        var safeGoal = goal == null ? GoalType.AUTOMATIC : goal;
        if (safeGoal == GoalType.AUTOMATIC || safeGoal == GoalType.CUSTOM)
            return new GoalRecommendationContext(safeGoal,
                    GoalRelation.AUTOMATIC,
                    get(296), null);

        var name = displayName(safeGoal);
        if (requiresMembers(safeGoal) && membership != Membership.P2P)
            return new GoalRecommendationContext(safeGoal,
                    membership == Membership.UNKNOWN
                            ? GoalRelation.CHECK_NEEDED
                            : GoalRelation.FALLBACK,
                    membership == Membership.UNKNOWN
                            ? get(1229) + name + "."
                            : name + get(297),
                    null);

        if (recommendation == null)
            return new GoalRecommendationContext(safeGoal,
                    GoalRelation.CHECK_NEEDED,
                    get(298) + name + ".",
                    null);
        if (recommendation.confidence == Confidence.BLOCKED)
            return new GoalRecommendationContext(safeGoal,
                    GoalRelation.BLOCKED,
                    get(1230) + name + " yet.",
                    null);

        var provenance = recommendation.goalProvenance;
        if (provenance != null
                && provenance.proves(safeGoal, recommendation.id))
        {
            if (recommendation.confidence
                    == Confidence.CHECK_NEEDED)
                return new GoalRecommendationContext(safeGoal,
                        GoalRelation.CHECK_NEEDED,
                        get(1231) + name + ".",
                        provenance);
            boolean direct = provenance.getRelationship()
                    == GoalRelation.DIRECT;
            return new GoalRecommendationContext(safeGoal,
                    provenance.getRelationship(),
                    direct ? get(1232) + name + "."
                            : get(1233) + name + ".",
                    provenance);
        }
        return new GoalRecommendationContext(safeGoal,
                GoalRelation.FALLBACK,
                "", null);
    }

    public boolean hasProvenRelationship() { return provenance != null; }

    static String displayName(GoalType goal)
    {
        if (goal == null) return "Automatic";
        return goal.toString();
    }

    static boolean requiresMembers(GoalType goal)
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
