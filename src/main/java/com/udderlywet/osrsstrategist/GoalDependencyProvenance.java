package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A validated dependency path connecting one recommendation to one selected goal. */
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
                    "Goal provenance requires a goal, action, relationship, and dependency path");
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

    public GoalType getGoal() { return goal; }
    public GoalRecommendationRelationship getRelationship() { return relationship; }
    public String getRecommendationId() { return recommendationId; }
    public List<String> getPath() { return path; }

    public boolean proves(GoalType selectedGoal, String actionId)
    {
        return goal == selectedGoal && recommendationId.equals(actionId)
                && path.size() >= 2;
    }

    public String compactPath()
    {
        return String.join(" → ", path);
    }
}
