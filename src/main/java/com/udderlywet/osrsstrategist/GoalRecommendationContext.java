package com.udderlywet.osrsstrategist;

/** Short, player-facing explanation of a recommendation's goal relationship. */
public final class GoalRecommendationContext
{
    private final GoalType goal;
    private final GoalRecommendationRelationship relationship;
    private final String status;

    private GoalRecommendationContext(GoalType goal,
            GoalRecommendationRelationship relationship, String status)
    {
        this.goal = goal == null ? GoalType.AUTOMATIC : goal;
        this.relationship = relationship == null
                ? GoalRecommendationRelationship.AUTOMATIC : relationship;
        this.status = status == null ? "" : status;
    }

    public static GoalRecommendationContext assess(GoalType goal,
            Recommendation recommendation, MembershipStatus membership)
    {
        GoalType safeGoal = goal == null ? GoalType.AUTOMATIC : goal;
        if (safeGoal == GoalType.AUTOMATIC || safeGoal == GoalType.CUSTOM)
            return new GoalRecommendationContext(safeGoal,
                    GoalRecommendationRelationship.AUTOMATIC,
                    "Compass is choosing the best overall move.");

        String name = displayName(safeGoal);
        if (requiresMembers(safeGoal) && membership != MembershipStatus.P2P)
            return new GoalRecommendationContext(safeGoal,
                    membership == MembershipStatus.UNKNOWN
                            ? GoalRecommendationRelationship.CHECK_NEEDED
                            : GoalRecommendationRelationship.FALLBACK,
                    membership == MembershipStatus.UNKNOWN
                            ? "Confirm membership before advancing " + name + "."
                            : name + " requires members content, so Compass is recommending useful F2P progression for now.");

        if (recommendation == null)
            return new GoalRecommendationContext(safeGoal,
                    GoalRecommendationRelationship.CHECK_NEEDED,
                    "Compass needs more account evidence before advancing " + name + ".");
        if (recommendation.getConfidence() == RecommendationConfidence.BLOCKED)
            return new GoalRecommendationContext(safeGoal,
                    GoalRecommendationRelationship.BLOCKED,
                    "This account cannot safely advance " + name + " yet.");

        double contribution = RecommendationIntelligenceService.goalValue(
                recommendation, safeGoal);
        if (contribution >= 28.0)
            return new GoalRecommendationContext(safeGoal,
                    recommendation.getConfidence() == RecommendationConfidence.CHECK_NEEDED
                            ? GoalRecommendationRelationship.CHECK_NEEDED
                            : GoalRecommendationRelationship.DIRECT,
                    recommendation.getConfidence() == RecommendationConfidence.CHECK_NEEDED
                            ? "Confirm the remaining state for this " + name + " step."
                            : "This directly advances " + name + ".");
        if (contribution > 0.0)
            return new GoalRecommendationContext(safeGoal,
                    recommendation.getConfidence() == RecommendationConfidence.CHECK_NEEDED
                            ? GoalRecommendationRelationship.CHECK_NEEDED
                            : GoalRecommendationRelationship.PREREQUISITE,
                    recommendation.getConfidence() == RecommendationConfidence.CHECK_NEEDED
                            ? "Confirm this prerequisite before advancing " + name + "."
                            : "This advances a " + name + " prerequisite.");
        return new GoalRecommendationContext(safeGoal,
                GoalRecommendationRelationship.FALLBACK,
                "Useful progression while no safe " + name + " step can lead.");
    }

    public GoalType getGoal() { return goal; }
    public GoalRecommendationRelationship getRelationship() { return relationship; }
    public String getStatus() { return status; }
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
