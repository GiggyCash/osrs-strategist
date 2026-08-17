package com.udderlywet.osrsstrategist;

import java.util.List;
import javax.inject.Singleton;

/**
 * Hard gate for the primary DO NEXT slot.
 *
 * <p>A primary recommendation must be verified, requirement-clean, and have a
 * concrete structured action. A vague method description is never accepted as
 * a substitute for executable guidance.</p>
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
            if (plan.getMethod() == null
                    || hasUnresolvedRequirements(plan.getRequirementChecks()))
            {
                return false;
            }
        }

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
