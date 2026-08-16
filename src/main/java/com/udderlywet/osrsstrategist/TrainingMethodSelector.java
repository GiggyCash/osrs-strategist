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
    private final ExpandedTrainingMethodCatalog expandedCatalog;
    private final RequirementEvidenceEngine requirementEvidenceEngine;

    @Inject
    public TrainingMethodSelector(
            TrainingMethodDatabase database,
            ExpandedTrainingMethodCatalog expandedCatalog,
            RequirementEvidenceEngine requirementEvidenceEngine)
    {
        this.database = database;
        this.expandedCatalog = expandedCatalog;
        this.requirementEvidenceEngine = requirementEvidenceEngine;
    }

    /** Compatibility constructor retained for existing tests/callers. */
    public TrainingMethodSelector(
            TrainingMethodDatabase database,
            RequirementEvidenceEngine requirementEvidenceEngine)
    {
        this(database, null, requirementEvidenceEngine);
    }

    /** Compatibility constructor retained for existing tests. */
    public TrainingMethodSelector(TrainingMethodDatabase database)
    {
        this(database, null, null);
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
        List<TrainingMethod> methods = new ArrayList<>(database.methodsFor(skill));
        if (expandedCatalog != null)
        {
            methods.addAll(expandedCatalog.methodsFor(skill));
        }
        MembershipStatus membershipStatus = membershipStatus(data);
        AccountMode accountMode = accountMode(data);
        TrainingMethod bestMethod = null;
        List<RequirementCheck> bestChecks = Collections.emptyList();
        RecommendationConfidence bestConfidence = RecommendationConfidence.CHECK_NEEDED;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (TrainingMethod method : methods)
        {
            if (!method.supportsLevel(currentLevel)
                    || !wildernessAllowed(method, accountMode, allowWildernessMethods)
                    || !modeAllowsMethod(method, accountMode)
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

            double score = method.scoreFor(strategyMode, sessionIntent)
                    + accountModeScore(method, accountMode);
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
                buildExplanation(bestMethod, strategyMode, sessionIntent, accountMode),
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

    private static AccountMode accountMode(StrategyDataBundle data)
    {
        if (data == null || data.getAccount() == null)
        {
            return AccountMode.UNKNOWN;
        }
        return AccountMode.fromTypeCode(data.getAccount().getAccountTypeCode());
    }

    private static boolean wildernessAllowed(
            TrainingMethod method,
            AccountMode mode,
            boolean settingEnabled)
    {
        if (!method.isWilderness()) return true;
        if (mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
        {
            return false;
        }
        return settingEnabled;
    }

    private static boolean modeAllowsMethod(TrainingMethod method, AccountMode mode)
    {
        if (mode != AccountMode.ULTIMATE_IRONMAN) return true;
        String id = method.getId();
        if (id == null) return true;
        return !"herblore_bank".equals(id)
                && !"crafting_banked".equals(id)
                && !"smithing_banked".equals(id)
                && !"cooking_banked".equals(id);
    }

    private static double accountModeScore(TrainingMethod method, AccountMode mode)
    {
        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            String id = method.getId() == null ? "" : method.getId();
            if (id.contains("gotr") || id.contains("wintertodt")
                    || id.contains("tempoross") || id.contains("sep")
                    || id.contains("stars") || id.contains("herbiboar")
                    || id.contains("karambwan") || id.contains("mahogany_homes"))
            {
                return 2.5;
            }
        }
        if (mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
        {
            if (method.getAttentionLevel() == AttentionLevel.AFK) return -1.5;
            if (method.getAttentionLevel() == AttentionLevel.MODERATE) return 1.0;
        }
        return 0.0;
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
            SessionIntent sessionIntent,
            AccountMode accountMode)
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
        if (accountMode == AccountMode.ULTIMATE_IRONMAN)
        {
            reason.append(" UIM bank assumptions are disabled.");
        }
        if (accountMode == AccountMode.HARDCORE_IRONMAN
                || accountMode == AccountMode.HARDCORE_GROUP_IRONMAN)
        {
            reason.append(" Hardcore safety policy is active.");
        }
        return reason.toString();
    }

    private static String pretty(String value)
    {
        String lower = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
