package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** A validated dependency path connecting one recommendation to one selected goal. */
@Getter
public final class GoalDependencyProvenance
{
    private final GoalType goal;
    private final GoalRecommendationRelationship relationship;
    private final String recommendationId;
    private final List<String> path;

    private GoalDependencyProvenance(GoalType goal,
            GoalRecommendationRelationship relationship,
            String recommendationId, List<String> path)
    {
        if (goal == null || goal == GoalType.AUTOMATIC
                || relationship != GoalRecommendationRelationship.DIRECT
                && relationship != GoalRecommendationRelationship.PREREQUISITE
                || recommendationId == null || recommendationId.trim().isEmpty()
                || path == null || path.size() < 2)
        {
            throw new IllegalArgumentException(
                    Text.get(263));
        }
        this.goal = goal;
        this.relationship = relationship;
        this.recommendationId = recommendationId;
        this.path = Collections.unmodifiableList(new ArrayList<>(path));
    }

    public static GoalDependencyProvenance direct(GoalType goal,
            String recommendationId, List<String> path)
    {
        return new GoalDependencyProvenance(goal,
                GoalRecommendationRelationship.DIRECT,
                recommendationId, path);
    }

    public static GoalDependencyProvenance prerequisite(GoalType goal,
            String recommendationId, List<String> path)
    {
        return new GoalDependencyProvenance(goal,
                GoalRecommendationRelationship.PREREQUISITE,
                recommendationId, path);
    }


    public boolean proves(GoalType selectedGoal, String actionId)
    {
        return goal == selectedGoal && recommendationId.equals(actionId)
                && path.size() >= 2;
    }

    public String compactPath()
    {
        return String.join(" → ", path);
    }

    /** Causal player copy derived only from the validated dependency path. */
    public String playerReason()
    {
        String goalName = path.isEmpty() ? goal.toString() : path.get(0);
        String action = path.get(path.size() - 1);
        if (relationship == GoalRecommendationRelationship.DIRECT)
            return action + " directly advances your " + goalName + " goal.";
        if (path.size() >= 3)
        {
            String parent = path.get(path.size() - 2);
            return action + " is required for " + parent
                    + ", which is on your " + goalName + " path.";
        }
        return action + " is required for your " + goalName + " goal.";
    }
}
