package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Converts any selected method into the same reusable checklist model. */
@Singleton
public class MethodGuidanceService
{
    private final FarmingRunPlanner farmingRunPlanner;

    @Inject
    public MethodGuidanceService(FarmingRunPlanner farmingRunPlanner)
    {
        this.farmingRunPlanner = farmingRunPlanner;
    }

    public GuidanceChecklist build(
            Recommendation recommendation,
            StrategyDataBundle data)
    {
        if (recommendation == null) return null;
        TrainingPlan plan = recommendation.getTrainingPlan();
        if (plan == null || plan.getMethod() == null) return null;

        TrainingMethod method = plan.getMethod();
        if (method.getSkill() == Skill.FARMING)
        {
            return farmingRunPlanner.build(data, recommendation.getId());
        }

        List<GuidanceStep> steps = new ArrayList<>();
        for (RequirementCheck check : plan.getRequirementChecks())
        {
            steps.add(new GuidanceStep(
                    check.getId(), check.getLabel(), check.getEvidence(),
                    convert(check.getState())));
        }

        if (steps.isEmpty())
        {
            steps.add(new GuidanceStep(
                    "method:ready", "Method ready",
                    "All known prerequisites for this method are ready.",
                    GuidanceStepState.COMPLETE));
        }

        return new GuidanceChecklist(
                recommendation.getId(), method.getName(),
                plan.getWhyThisMethod(), steps);
    }

    private GuidanceStepState convert(RequirementState state)
    {
        if (state == RequirementState.VERIFIED) return GuidanceStepState.COMPLETE;
        if (state == RequirementState.BLOCKED) return GuidanceStepState.BLOCKED;
        return GuidanceStepState.CHECK_NEEDED;
    }
}
