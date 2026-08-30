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

        CandidateSafetyEvidence evidence = recommendation.getSafetyEvidence();
        RecommendationGuidance guidance = recommendation.getGuidance();
        if (profile != null && profile.getBankingBehavior()
                        == MethodBankingBehavior.CONVENTIONAL_BANK_LOOP
                || guidance != null && guidance.getBankingBehavior()
                        == MethodBankingBehavior.CONVENTIONAL_BANK_LOOP)
        {
            evidence = evidence.requiringConventionalBank();
        }
        if (guidance != null && guidance.getStorageCapability() != null)
        {
            StorageCapability capability = guidance.getStorageCapability();
            UimStorageDecision decision = guidance.getStorageDecision();
            boolean storageUnverified = decision == null
                    || !decision.isAllowed()
                    || decision.getConfidence()
                            != RecommendationConfidence.VERIFIED;
            boolean incompleteDangerDisclosure =
                    UimStorageMechanics.isDangerous(capability)
                    && (guidance.getRiskDisclosure() == null
                    || !guidance.getRiskDisclosure()
                            .isAcknowledgementRequired());
            if (storageUnverified
                    || UimStorageMechanics.isTooGenericToRecommend(capability)
                    || incompleteDangerDisclosure)
                evidence = evidence.withUnverifiedDangerousStorage();
        }
        return recommendation.withSafetyEvidence(evidence);
    }
}
