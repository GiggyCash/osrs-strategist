package compass;

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
                || context.data() == null
                || context.data().account() == null) return recommendation;
        var merged = recommendation.getStrategicValue();
        for (InfrastructureMilestone definition : catalog.all())
        {
            InfrastructureValueAssessment assessment = values.assess(
                    definition.id, context);
            if (assessment.getState() == InfrastructureMilestoneState.COMPLETE
                    || assessment.getState()
                            == InfrastructureMilestoneState.NOT_APPLICABLE)
                continue;
            if (!matches(recommendation, definition, context)) continue;
            double utility = assessment.getStrategicValue().ordinal()
                    / (double) StrategicPriority.CRITICAL.ordinal();
            merged = merged.merge(StrategicValue.builder()
                    .infrastructureValue(utility)
                    .accountModeFit(utility * 0.6)
                    .unlockValue(utility * 0.5)
                    .evidence("infrastructure:" + definition.id)
                    .build());
        }
        return recommendation.withStrategicValue(merged);
    }

    private static boolean matches(Recommendation recommendation,
            InfrastructureMilestone definition,
            StrategyContext context)
    {
        var training = recommendation.plan();
        Skill skill = training == null || training.method() == null
                ? null : training.method().getSkill();
        int current = skill == null ? 0 : context.data().account()
                .level(skill);
        var required = definition.getRequiredSkills().getOrDefault(skill, 0);
        if (skill != null && required > 0
                && current < required
                && recommendation.getTargetLevel()
                        >= required) return true;

        for (String quest : definition.getRequiredQuests().keySet())
            if (recommendation.id != null
                    && recommendation.id.equals("quest:" + slug(quest)))
                return true;
        return recommendation.id != null
                && recommendation.id.equals(
                        "infrastructure:" + definition.id);
    }

    private static String slug(String value)
    {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
