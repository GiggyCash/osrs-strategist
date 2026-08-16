package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Chooses the best training method for one skill.
 *
 * <p>The selector receives the whole StrategyDataBundle so method choice and
 * readiness can be based on the same verified account state. Requirement
 * evidence is evaluated here, never in the Swing UI.</p>
 */
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

    /** Compatibility constructor retained for focused unit tests. */
    public TrainingMethodSelector(TrainingMethodDatabase database)
    {
        this(database, null);
    }

    /**
     * Compatibility overload used by focused unit tests and older callers.
     */
    public TrainingPlan select(
            Skill skill,
            int currentLevel,
            StrategyMode strategyMode,
            SessionIntent sessionIntent)
    {
        return select(
                null,
                skill,
                currentLevel,
                strategyMode,
                sessionIntent
        );
    }

    public TrainingPlan select(
            StrategyDataBundle data,
            Skill skill,
            int currentLevel,
            StrategyMode strategyMode,
            SessionIntent sessionIntent)
    {
        List<TrainingMethod> methods = database.methodsFor(skill);
        MembershipStatus membershipStatus = membershipStatus(data);

        TrainingMethod best = methods.stream()
                .filter(method -> method.supportsLevel(currentLevel))
                .filter(method -> ContentAccessRules.isMethodAvailable(
                        method,
                        membershipStatus
                ))
                .filter(method -> method.getConfidence()
                        != RecommendationConfidence.BLOCKED)
                .max(Comparator.comparingDouble(
                        method -> method.scoreFor(
                                strategyMode,
                                sessionIntent
                        )
                ))
                .orElse(null);

        if (best == null)
        {
            return null;
        }

        List<RequirementCheck> checks = requirementEvidenceEngine == null
                ? Collections.emptyList()
                : requirementEvidenceEngine.evaluate(data, best);

        RecommendationConfidence confidence =
                assessConfidence(best, checks);

        return new TrainingPlan(
                best,
                buildExplanation(
                        best,
                        strategyMode,
                        sessionIntent
                ),
                confidence,
                checks
        );
    }

    private static MembershipStatus membershipStatus(
            StrategyDataBundle data)
    {
        if (data == null || data.getAccount() == null)
        {
            return MembershipStatus.UNKNOWN;
        }

        return data.getAccount().getMembershipStatus();
    }

    /**
     * Confidence is the aggregate of concrete checks. One known blocker blocks
     * the plan; one unknown keeps it Check Needed; all verified checks upgrade
     * the plan to Verified even if its static catalog entry began conservatively.
     */
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
                && method.getConfidence()
                == RecommendationConfidence.VERIFIED)
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

        return reason.toString();
    }

    private static String pretty(String value)
    {
        String lower = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0))
                + lower.substring(1);
    }
}
