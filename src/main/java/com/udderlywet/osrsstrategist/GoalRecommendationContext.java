package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Short, player-facing explanation of a recommendation's goal relationship. */
public final class GoalRecommendationContext
{
    @Getter
    private final GoalType goal;
    @Getter
    private final GoalRecommendationRelationship relationship;
    @Getter
    private final String status;
    @Getter
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
                    "Compass is choosing the best overall move.", null);

        String name = displayName(safeGoal);
        if (requiresMembers(safeGoal) && membership != MembershipStatus.P2P)
            return new GoalRecommendationContext(safeGoal,
                    membership == MembershipStatus.UNKNOWN
                            ? GoalRecommendationRelationship.CHECK_NEEDED
                            : GoalRecommendationRelationship.FALLBACK,
                    membership == MembershipStatus.UNKNOWN
                            ? "Confirm membership before advancing " + name + "."
                            : name + " requires members content, so Compass is recommending useful F2P progression for now.",
                    null);

        if (recommendation == null)
            return new GoalRecommendationContext(safeGoal,
                    GoalRecommendationRelationship.CHECK_NEEDED,
                    "Compass needs more account evidence before advancing " + name + ".",
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
