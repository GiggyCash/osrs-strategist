package compass;

import java.util.*;

import lombok.Getter;

/** A validated dependency path connecting one recommendation to one selected goal. */
@Getter
public final class GoalProvenance
{
    private final GoalType goal;
    private final GoalRecommendationRelationship relationship;
    private final String recommendationId;
    private final List<String> path;

    private GoalProvenance(GoalType goal,
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

    public static GoalProvenance direct(GoalType goal,
            String recommendationId, List<String> path)
    {
        return new GoalProvenance(goal,
                GoalRecommendationRelationship.DIRECT,
                recommendationId, path);
    }

    public static GoalProvenance prerequisite(GoalType goal,
            String recommendationId, List<String> path)
    {
        return new GoalProvenance(goal,
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
        var goalName = path.isEmpty() ? goal.toString() : path.get(0);
        var action = path.get(path.size() - 1);
        if (relationship == GoalRecommendationRelationship.DIRECT)
            return action + Text.get(1226) + goalName + " goal.";
        if (path.size() >= 3)
        {
            var parent = path.get(path.size() - 2);
            return action + Text.get(1708) + parent
                    + Text.get(1227) + goalName + " path.";
        }
        return action + Text.get(1228) + goalName + " goal.";
    }
}
