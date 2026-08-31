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

        SafetyEvidence evidence = recommendation.getSafetyEvidence();
        Guidance guidance = recommendation.getGuidance();
        if (plan != null)
        {
            TrainingMethod method = plan.getMethod();
            int current = recommendation.getCurrentLevel();
            int stageTarget = recommendation.getCurrentExecutionTargetLevel();
            boolean invalid = method == null
                    || blank(method.getName())
                    || current <= 0
                    || !method.supportsLevel(current)
                    || stageTarget <= current
                    || recommendation.getTargetLevel() > 0
                        && stageTarget > recommendation.getTargetLevel()
                    || context != null && context.data() != null
                        && context.data().account() != null
                        && !ContentAccessRules.isMethodAvailable(method,
                                context.data().account()
                                        .getMembershipStatus())
                    || guidance == null
                    || blank(guidance.getAction())
                    || blank(guidance.getLocation());
            if (invalid) evidence = evidence.withInvalidCurrentExecution();
        }
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
                            != Confidence.VERIFIED;
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

    private static boolean blank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
