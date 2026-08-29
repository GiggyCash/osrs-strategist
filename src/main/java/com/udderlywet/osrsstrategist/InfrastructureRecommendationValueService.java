package com.udderlywet.osrsstrategist;

import java.util.Locale;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Attaches infrastructure utility to its actual typed prerequisite actions. */
@Singleton
public final class InfrastructureRecommendationValueService
{
    private final InfrastructureMilestoneCatalog catalog;
    private final InfrastructureUnlockValueService values;

    public InfrastructureRecommendationValueService()
    {
        this(new InfrastructureMilestoneCatalog(),
                new InfrastructureUnlockValueService());
    }

    InfrastructureRecommendationValueService(
            InfrastructureMilestoneCatalog catalog,
            InfrastructureUnlockValueService values)
    {
        this.catalog = catalog;
        this.values = values;
    }

    public Recommendation attach(
            Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null || context == null
                || context.getData() == null
                || context.getData().getAccount() == null) return recommendation;
        RecommendationStrategicValue merged = recommendation.getStrategicValue();
        for (InfrastructureMilestoneDefinition definition : catalog.all())
        {
            InfrastructureValueAssessment assessment = values.assess(
                    definition.getId(), context);
            if (assessment.getState() == InfrastructureMilestoneState.COMPLETE
                    || assessment.getState()
                            == InfrastructureMilestoneState.NOT_APPLICABLE)
                continue;
            if (!matches(recommendation, definition, context)) continue;
            double utility = assessment.getStrategicValue().ordinal()
                    / (double) StrategicPriority.CRITICAL.ordinal();
            merged = merged.merge(RecommendationStrategicValue.builder()
                    .infrastructureValue(utility)
                    .accountModeFit(utility * 0.6)
                    .unlockValue(utility * 0.5)
                    .evidence("infrastructure:" + definition.getId())
                    .build());
        }
        return recommendation.withStrategicValue(merged);
    }

    private static boolean matches(Recommendation recommendation,
            InfrastructureMilestoneDefinition definition,
            StrategyContext context)
    {
        TrainingPlan training = recommendation.getTrainingPlan();
        Skill skill = training == null || training.getMethod() == null
                ? null : training.getMethod().getSkill();
        int current = skill == null ? 0 : context.getData().getAccount()
                .getSkillLevel(skill);
        int required = definition.getRequiredSkills().getOrDefault(skill, 0);
        if (skill != null && required > 0
                && current < required
                && recommendation.getTargetLevel()
                        >= required) return true;

        for (String quest : definition.getRequiredQuests().keySet())
            if (recommendation.getId() != null
                    && recommendation.getId().equals("quest:" + slug(quest)))
                return true;
        return recommendation.getId() != null
                && recommendation.getId().equals(
                        "infrastructure:" + definition.getId());
    }

    private static String slug(String value)
    {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
