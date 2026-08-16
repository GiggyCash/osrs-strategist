package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

@Singleton
public class TrainingMethodSelector
{
    private final TrainingMethodDatabase database;
    private final RequirementEvidenceEngine requirementEvidenceEngine;

    @Inject
    public TrainingMethodSelector(
            TrainingMethodDatabase database,
            RequirementEvidenceEngine requirementEvidenceEngine)
    {
        this.database = database;
        this.requirementEvidenceEngine = requirementEvidenceEngine;
    }

    public TrainingMethodSelector(TrainingMethodDatabase database)
    {
        this(database, null);
    }

    public TrainingPlan select(
            Skill skill,
            int currentLevel,
            StrategyMode strategyMode,
            SessionIntent sessionIntent)
    {
        return select(null, skill, currentLevel, strategyMode,
                sessionIntent, false);
    }

    public TrainingPlan select(
            StrategyDataBundle data,
            Skill skill,
            int currentLevel,
            StrategyMode strategyMode,
            SessionIntent sessionIntent)
    {
        return select(data, skill, currentLevel, strategyMode,
                sessionIntent, false);
    }

    public TrainingPlan select(
            StrategyDataBundle data,
            Skill skill,
            int currentLevel,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            boolean allowWildernessMethods)
    {
        List<TrainingMethod> methods = database.methodsFor(skill);
        MembershipStatus membershipStatus = membershipStatus(data);
        TrainingMethod bestMethod = null;
        List<RequirementCheck> bestChecks = Collections.emptyList();
        RecommendationConfidence bestConfidence = RecommendationConfidence.CHECK_NEEDED;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (TrainingMethod method : methods)
        {
            if (!method.supportsLevel(currentLevel)
                    || (!allowWildernessMethods && method.isWilderness())
                    || !ContentAccessRules.isMethodAvailable(method, membershipStatus)
                    || method.getConfidence() == RecommendationConfidence.BLOCKED)
            {
                continue;
            }

            List<RequirementCheck> checks = requirementEvidenceEngine == null
                    ? Collections.emptyList()
                    : requirementEvidenceEngine.evaluate(data, method);
            RecommendationConfidence confidence = assessConfidence(method, checks);

            if (confidence == RecommendationConfidence.BLOCKED)
            {
                continue;
            }

            double score = method.scoreFor(strategyMode, sessionIntent);
            if (bestMethod == null || score > bestScore)
            {
                bestMethod = method;
                bestChecks = checks;
                bestConfidence = confidence;
                bestScore = score;
            }
        }

        if (bestMethod == null)
        {
            return null;
        }

        return new TrainingPlan(
                bestMethod,
                buildExplanation(bestMethod, strategyMode, sessionIntent),
                bestConfidence,
                bestChecks
        );
    }

    private static MembershipStatus membershipStatus(StrategyDataBundle data)
    {
        if (data == null || data.getAccount() == null)
        {
            return MembershipStatus.UNKNOWN;
        }
        return data.getAccount().getMembershipStatus();
    }

    private RecommendationConfidence assessConfidence(
            TrainingMethod method,
            List<RequirementCheck> checks)
    {
        if (method.getConfidence() == RecommendationConfidence.BLOCKED)
        {
            return RecommendationConfidence.BLOCKED;
        }
        if (checks != null && !checks.isEmpty())
        {
            boolean hasUnknown = false;
            for (RequirementCheck check : checks)
            {
                if (check.getState() == RequirementState.BLOCKED)
                {
                    return RecommendationConfidence.BLOCKED;
                }
                if (check.getState() == RequirementState.CHECK_NEEDED)
                {
                    hasUnknown = true;
                }
            }
            return hasUnknown
                    ? RecommendationConfidence.CHECK_NEEDED
                    : RecommendationConfidence.VERIFIED;
        }
        if (method.getRequirements().isEmpty()
                && method.getConfidence() == RecommendationConfidence.VERIFIED)
        {
            return RecommendationConfidence.VERIFIED;
        }
        return RecommendationConfidence.CHECK_NEEDED;
    }

    private String buildExplanation(
            TrainingMethod method,
            StrategyMode strategyMode,
            SessionIntent sessionIntent)
    {
        StringBuilder reason = new StringBuilder();
        reason.append("Selected for ")
                .append(pretty(strategyMode.name()))
                .append(" strategy");
        if (sessionIntent != SessionIntent.PICK_FOR_ME)
        {
            reason.append(" and ")
                    .append(pretty(sessionIntent.name()))
                    .append(" sessions");
        }
        reason.append(". Attention: ")
                .append(pretty(method.getAttentionLevel().name()))
                .append(".");
        if (method.isWilderness())
        {
            reason.append(" Wilderness method enabled by this character's settings.");
        }
        return reason.toString();
    }

    private static String pretty(String value)
    {
        String lower = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
