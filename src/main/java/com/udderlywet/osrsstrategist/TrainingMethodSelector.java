package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
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
    private final ExpandedTrainingMethodCatalog expandedCatalog;
    private final TrainingMethodPolicy methodPolicy;

    @Inject
    public TrainingMethodSelector(
            TrainingMethodDatabase database,
            RequirementEvidenceEngine requirementEvidenceEngine,
            ExpandedTrainingMethodCatalog expandedCatalog,
            TrainingMethodPolicy methodPolicy)
    {
        this.database = database;
        this.requirementEvidenceEngine = requirementEvidenceEngine;
        this.expandedCatalog = expandedCatalog;
        this.methodPolicy = methodPolicy;
    }

    public TrainingMethodSelector(
            TrainingMethodDatabase database,
            RequirementEvidenceEngine requirementEvidenceEngine)
    {
        this(database, requirementEvidenceEngine,
                new ExpandedTrainingMethodCatalog(), new TrainingMethodPolicy());
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
        List<CuratedTrainingMethod> methods = candidates(data, skill);
        MembershipStatus membershipStatus = membershipStatus(data);
        TrainingMethod bestMethod = null;
        TrainingMethodMetadata bestMetadata = null;
        List<RequirementCheck> bestChecks = Collections.emptyList();
        RecommendationConfidence bestConfidence = RecommendationConfidence.CHECK_NEEDED;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (CuratedTrainingMethod candidate : methods)
        {
            TrainingMethod method = candidate.getMethod();
            TrainingMethodMetadata metadata = candidate.getMetadata();
            if (!method.supportsLevel(currentLevel)
                    || !ContentAccessRules.isMethodAvailable(method, membershipStatus)
                    || method.getConfidence() == RecommendationConfidence.BLOCKED
                    || !methodPolicy.isAllowed(data, method, metadata,
                    allowWildernessMethods))
            {
                continue;
            }

            List<RequirementCheck> checks = requirementEvidenceEngine == null
                    ? Collections.emptyList()
                    : requirementEvidenceEngine.evaluate(data, method);
            RecommendationConfidence confidence = assessConfidence(method, checks);
            if (confidence == RecommendationConfidence.BLOCKED) continue;

            double score = method.scoreFor(strategyMode, sessionIntent)
                    + methodPolicy.scoreAdjustment(
                    data, metadata, strategyMode, sessionIntent);
            if (bestMethod == null || score > bestScore)
            {
                bestMethod = method;
                bestMetadata = metadata;
                bestChecks = checks;
                bestConfidence = confidence;
                bestScore = score;
            }
        }

        if (bestMethod == null) return null;
        return new TrainingPlan(
                bestMethod,
                buildExplanation(bestMethod, bestMetadata,
                        strategyMode, sessionIntent, data),
                bestConfidence,
                bestChecks
        );
    }

    private List<CuratedTrainingMethod> candidates(
            StrategyDataBundle data,
            Skill skill)
    {
        List<CuratedTrainingMethod> candidates = new ArrayList<>();
        MembershipStatus membership = membershipStatus(data);

        // Legacy methods predate method-level F2P metadata, so an F2P account
        // uses only the curated catalog where every route is explicitly tagged.
        if (membership != MembershipStatus.F2P)
        {
            for (TrainingMethod method : database.methodsFor(skill))
            {
                candidates.add(new CuratedTrainingMethod(
                        method, TrainingMethodMetadata.legacy(method)));
            }
        }
        candidates.addAll(expandedCatalog.methodsFor(skill));
        return candidates;
    }

    private static MembershipStatus membershipStatus(StrategyDataBundle data)
    {
        if (data == null || data.getAccount() == null) return MembershipStatus.UNKNOWN;
        return data.getAccount().getMembershipStatus();
    }

    private RecommendationConfidence assessConfidence(
            TrainingMethod method,
            List<RequirementCheck> checks)
    {
        if (method.getConfidence() == RecommendationConfidence.BLOCKED)
            return RecommendationConfidence.BLOCKED;
        if (checks != null && !checks.isEmpty())
        {
            boolean hasUnknown = false;
            for (RequirementCheck check : checks)
            {
                if (check.getState() == RequirementState.BLOCKED)
                    return RecommendationConfidence.BLOCKED;
                if (check.getState() == RequirementState.CHECK_NEEDED) hasUnknown = true;
            }
            return hasUnknown
                    ? RecommendationConfidence.CHECK_NEEDED
                    : RecommendationConfidence.VERIFIED;
        }
        if (method.getRequirements().isEmpty()
                && method.getConfidence() == RecommendationConfidence.VERIFIED)
            return RecommendationConfidence.VERIFIED;
        return RecommendationConfidence.CHECK_NEEDED;
    }

    private String buildExplanation(
            TrainingMethod method,
            TrainingMethodMetadata metadata,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            StrategyDataBundle data)
    {
        StringBuilder reason = new StringBuilder();
        reason.append("Selected for ")
                .append(pretty(strategyMode.name()))
                .append(" strategy");
        if (sessionIntent != SessionIntent.PICK_FOR_ME)
            reason.append(" and ").append(pretty(sessionIntent.name())).append(" sessions");
        reason.append(". Attention: ").append(pretty(method.getAttentionLevel().name())).append(".");
        if (metadata != null)
            reason.append(" Method profile: ")
                    .append(pretty(metadata.getIntensity().name()))
                    .append(", ")
                    .append(pretty(metadata.getCostTier().name()))
                    .append(" cost.");
        AccountMode mode = data == null || data.getAccount() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(data.getAccount().getAccountTypeCode());
        if (mode != AccountMode.UNKNOWN)
            reason.append(" Account policy: ").append(pretty(mode.name())).append(".");
        if (method.isWilderness())
            reason.append(" Wilderness method enabled by this character's settings.");
        return reason.toString();
    }

    private static String pretty(String value)
    {
        if (value == null || value.isEmpty()) return "Unknown";
        String lower = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
