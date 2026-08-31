package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Resolves the next boundary of the method the player can execute now. */
@Singleton
public final class CurrentExecutionStageResolver
{
    private final RuneLiteSkillActionCatalog actions;
    private final MethodExecutionProfileCatalog profiles;

    @Inject
    public CurrentExecutionStageResolver(RuneLiteSkillActionCatalog actions,
            MethodExecutionProfileCatalog profiles)
    {
        this.actions = actions == null ? new RuneLiteSkillActionCatalog() : actions;
        this.profiles = profiles == null
                ? new MethodExecutionProfileCatalog() : profiles;
    }

    public CurrentExecutionStageResolver()
    {
        this(new RuneLiteSkillActionCatalog(),
                new MethodExecutionProfileCatalog());
    }

    public int resolve(TrainingPlan plan, int currentLevel,
            int objectiveTargetLevel)
    {
        int objective = Math.max(currentLevel + 1, objectiveTargetLevel);
        if (plan == null || plan.getMethod() == null) return objective;

        TrainingMethod method = plan.getMethod();
        int boundary = objective;
        if (method.getMaxLevel() >= currentLevel
                && method.getMaxLevel() < boundary)
        {
            boundary = method.getMaxLevel() + 1;
        }

        MethodExecutionProfile profile = profiles.forMethod(method.getId());
        if (profile == null || actions == null || method.getSkill() == null)
            return boundary;

        List<RuneLiteSkillActionDefinition> skillActions =
                actions.actionsFor(method.getSkill());
        for (RuneLiteSkillActionDefinition action : skillActions)
        {
            if (action == null || action.getLevel() <= currentLevel
                    || action.getLevel() >= boundary
                    || !matches(action, profile.getActionTerms()))
                continue;
            // An action unlock inside one route changes its output, XP, inputs,
            // or instructions. Stop before it so the next refresh resolves the
            // newly legal action instead of pricing it into today's step.
            boundary = action.getLevel();
        }
        return Math.max(currentLevel + 1, Math.min(objective, boundary));
    }

    private static boolean matches(RuneLiteSkillActionDefinition action,
            List<String> terms)
    {
        if (terms == null || terms.isEmpty()) return false;
        String haystack = normalize(action.getId()) + " "
                + normalize(action.getName()) + " "
                + normalize(action.getCategory());
        for (String term : terms)
            if (haystack.contains(normalize(term))) return true;
        return false;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
    }
}
