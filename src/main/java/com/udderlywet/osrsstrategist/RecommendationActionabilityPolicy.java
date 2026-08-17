package com.udderlywet.osrsstrategist;

import java.util.List;
import javax.inject.Singleton;

/**
 * Decides whether a recommendation is concrete enough to occupy the primary
 * DO NEXT slot.
 *
 * <p>The main card is a promise to the player: if Strategist places something
 * there, it must be ready enough to act on now. Candidates that still need
 * requirement discovery can remain useful alternatives, but they do not outrank
 * a verified training action merely because their raw score is higher.</p>
 */
@Singleton
public class RecommendationActionabilityPolicy
{
    public boolean canLeadQueue(Recommendation recommendation)
    {
        if (recommendation == null
                || recommendation.getConfidence() != RecommendationConfidence.VERIFIED)
        {
            return false;
        }

        TrainingPlan plan = recommendation.getTrainingPlan();
        if (plan != null)
        {
            if (plan.getMethod() == null || hasUnresolvedRequirements(plan.getRequirementChecks()))
            {
                return false;
            }

            RecommendationGuidance guidance = recommendation.getGuidance();
            if (guidance != null && hasText(guidance.getAction()))
            {
                return true;
            }

            return hasText(plan.getMethod().getInstructions());
        }

        // Non-skill candidates need structured action guidance before they are
        // allowed to become the primary recommendation. A paragraph explaining
        // why an activity is useful is not the same thing as telling the player
        // what to do next.
        RecommendationGuidance guidance = recommendation.getGuidance();
        return guidance != null && hasText(guidance.getAction());
    }

    public boolean mayAppearAsAlternative(Recommendation recommendation)
    {
        return recommendation != null
                && recommendation.getConfidence() != RecommendationConfidence.BLOCKED;
    }

    public int queuePriority(Recommendation recommendation)
    {
        if (canLeadQueue(recommendation)) return 2;
        if (mayAppearAsAlternative(recommendation)) return 1;
        return 0;
    }

    private static boolean hasUnresolvedRequirements(List<RequirementCheck> checks)
    {
        if (checks == null || checks.isEmpty()) return false;
        for (RequirementCheck check : checks)
        {
            if (check == null || check.getState() != RequirementState.VERIFIED)
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }
}
