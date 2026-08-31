package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Short, player-facing explanation of a recommendation's goal relationship. */
@Getter
public final class GoalRecommendationContext
{
    private final GoalType goal;
    private final GoalRecommendationRelationship relationship;
    private final String status;
    private final GoalDependencyProvenance provenance;

    private GoalRecommendationContext(GoalType goal,
            GoalRecommendationRelationship relationship, String status,
            GoalDependencyProvenance provenance)
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
        GoalType safeGoal = goal == null ? GoalType.AUTOMATIC : goal;
        if (safeGoal == GoalType.AUTOMATIC || safeGoal == GoalType.CUSTOM)
            return new GoalRecommendationContext(safeGoal,
                    GoalRecommendationRelationship.AUTOMATIC,
                    PlayerText.get("GRC1"), null);

        String name = displayName(safeGoal);
        if (requiresMembers(safeGoal) && membership != MembershipStatus.P2P)
            return new GoalRecommendationContext(safeGoal,
                    membership == MembershipStatus.UNKNOWN
                            ? GoalRecommendationRelationship.CHECK_NEEDED
                            : GoalRecommendationRelationship.FALLBACK,
                    membership == MembershipStatus.UNKNOWN
                            ? "Confirm membership before advancing " + name + "."
                            : name + PlayerText.get("GRC2"),
                    null);

        if (recommendation == null)
            return new GoalRecommendationContext(safeGoal,
                    GoalRecommendationRelationship.CHECK_NEEDED,
                    PlayerText.get("GRC3") + name + ".",
                    null);
        if (recommendation.getConfidence() == RecommendationConfidence.BLOCKED)
            return new GoalRecommendationContext(safeGoal,
                    GoalRecommendationRelationship.BLOCKED,
                    "This account cannot safely advance " + name + " yet.",
                    null);

        GoalDependencyProvenance provenance = recommendation.getGoalProvenance();
        if (provenance != null
                && provenance.proves(safeGoal, recommendation.getId()))
        {
            if (recommendation.getConfidence()
                    == RecommendationConfidence.CHECK_NEEDED)
                return new GoalRecommendationContext(safeGoal,
                        GoalRecommendationRelationship.CHECK_NEEDED,
                        "Prepare the next step toward " + name + ".",
                        provenance);
            boolean direct = provenance.getRelationship()
                    == GoalRecommendationRelationship.DIRECT;
            return new GoalRecommendationContext(safeGoal,
                    provenance.getRelationship(),
                    direct ? "Directly advances " + name + "."
                            : "Advances the proven path to " + name + ".",
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
