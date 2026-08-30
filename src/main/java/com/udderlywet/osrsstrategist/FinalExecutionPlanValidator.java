package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/**
 * Defense-in-depth validation after method, resources, inventory, location and
 * guidance have all been resolved.
 */
@Singleton
public final class FinalExecutionPlanValidator
{
    public Recommendation validate(Recommendation recommendation,
            StrategyContext context)
    {
        if (recommendation == null) return null;
        TrainingPlan plan = recommendation.getTrainingPlan();
        MethodStrategyProfile profile = plan == null
                ? null : plan.getStrategyProfile();
        if (profile == null) return recommendation;

        CandidateSafetyEvidence evidence = recommendation.getSafetyEvidence();
        RecommendationGuidance guidance = recommendation.getGuidance();
        if (profile.getBankingBehavior()
                        == MethodBankingBehavior.CONVENTIONAL_BANK_LOOP
                || guidance != null && guidance.getBankingBehavior()
                        == MethodBankingBehavior.CONVENTIONAL_BANK_LOOP)
        {
            evidence = evidence.requiringConventionalBank();
        }
        return recommendation.withSafetyEvidence(evidence);
    }
}
