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
        RecommendationGuidance guidance = recommendation.getGuidance();
        if (method.getSkill() == Skill.FARMING && guidance == null)
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

        String bring = guidance == null ? null
                : RecommendationPresentation.compactSentence(
                        guidance.getSupplies(), 120);
        String where = guidance == null ? null
                : RecommendationPresentation.compactSentence(
                        guidance.getLocation(), 110);
        String action = guidance == null
                ? method.getInstructions()
                : guidance.getAction();
        action = RecommendationPresentation.compactSentence(action, 135);
        String progress = guidance != null
                && guidance.getProgress() != null
                && !guidance.getProgress().trim().isEmpty()
                ? guidance.getProgress()
                : recommendation.getCurrentLevel() > 0
                && recommendation.getCurrentExecutionTargetLevel()
                        > recommendation.getCurrentLevel()
                ? "Level " + recommendation.getCurrentLevel() + " → "
                        + recommendation.getCurrentExecutionTargetLevel() : null;
        String important = guidance == null ? null
                : guidance.getRiskDisclosure() != null
                ? guidance.getRiskDisclosure().getHeading() + ": "
                        + guidance.getRiskDisclosure().getMessage()
                : criticalNote(guidance.getNote());

        return new GuidanceChecklist(
                recommendation.getId(), method.getName(),
                plan.getWhyThisMethod(), steps, bring, where, action,
                progress, important);
    }

    private static String criticalNote(String note)
    {
        if (note == null || note.trim().isEmpty()) return null;
        String lower = note.toLowerCase(java.util.Locale.ROOT);
        if (!(lower.contains("wilderness") || lower.contains("hardcore")
                || lower.contains("uim") || lower.contains("iron")
                || lower.contains("restricted") || lower.contains("mandatory")
                || lower.contains("required protection")
                || lower.contains("irreversible"))) return null;
        return RecommendationPresentation.compactSentence(note, 135);
    }

    private GuidanceStepState convert(RequirementState state)
    {
        if (state == RequirementState.VERIFIED) return GuidanceStepState.COMPLETE;
        if (state == RequirementState.BLOCKED) return GuidanceStepState.BLOCKED;
        return GuidanceStepState.CHECK_NEEDED;
    }
}
