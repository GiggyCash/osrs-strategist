package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/**
 * Hard gate for the primary DO NEXT slot.
 *
 * <p>The primary card must always contain an executable next action. A fully
 * verified route can lead immediately. A route with only ordinary preparation
 * outstanding may also lead when its guidance explains that preparation. True
 * unknown access, quest, build, or unlock requirements can never lead.</p>
 */
@Singleton
public class RecommendationActionabilityPolicy
{
    public boolean canLeadQueue(Recommendation recommendation)
    {
        if (recommendation == null
                || recommendation.getConfidence() == RecommendationConfidence.BLOCKED)
        {
            return false;
        }

        TrainingPlan plan = recommendation.getTrainingPlan();
        RecommendationGuidance guidance = recommendation.getGuidance();

        if (plan == null)
        {
            // Non-skill candidates must be fully verified and structured before
            // they can displace a concrete training action.
            return recommendation.getConfidence() == RecommendationConfidence.VERIFIED
                    && guidance != null
                    && hasText(guidance.getAction());
        }

        if (plan.getMethod() == null || guidance == null
                || !hasText(guidance.getAction()))
        {
            return false;
        }

        if (recommendation.getConfidence() == RecommendationConfidence.VERIFIED)
        {
            return !RequirementActionability.hasHardUnresolvedRequirement(plan);
        }

        // CHECK_NEEDED is allowed only when every unresolved check is ordinary
        // preparation and the guidance explicitly covers supplies/setup.
        return RequirementActionability.isActionablePreparation(plan, guidance);
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

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }
}
