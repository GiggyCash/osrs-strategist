package com.udderlywet.osrsstrategist;

import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Chooses the best training method for one skill.
 *
 * <p>The selector accepts the entire StrategyDataBundle even though the first
 * generation of scoring only uses part of it. That is deliberate: bank state,
 * account mode, equipment, quests, transport, GIM storage, UIM capabilities,
 * and other verified observations can be added to method scoring without
 * changing this API or rebuilding the recommendation pipeline.</p>
 */
@Singleton
public class TrainingMethodSelector
{
    private final TrainingMethodDatabase database;

    @Inject
    public TrainingMethodSelector(TrainingMethodDatabase database)
    {
        this.database = database;
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

        RecommendationConfidence confidence =
                assessConfidence(data, best);

        return new TrainingPlan(
                best,
                buildExplanation(
                        best,
                        strategyMode,
                        sessionIntent
                ),
                confidence
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
     * Starter confidence evaluation. Generic method definitions remain
     * CHECK_NEEDED until a reader/evaluator can prove their requirements.
     * Future requirement evaluators will plug in here rather than into the UI.
     */
    private RecommendationConfidence assessConfidence(
            StrategyDataBundle data,
            TrainingMethod method)
    {
        if (method.getConfidence()
                == RecommendationConfidence.BLOCKED)
        {
            return RecommendationConfidence.BLOCKED;
        }

        if (method.getRequirements().isEmpty()
                && method.getConfidence()
                == RecommendationConfidence.VERIFIED)
        {
            return RecommendationConfidence.VERIFIED;
        }

        // Having a full data bundle is not the same as having verified every
        // requirement inside it. Never upgrade confidence merely because data
        // exists; a requirement evaluator must explicitly prove readiness.
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

        if (!method.getRequirements().isEmpty())
        {
            reason.append(" Check: ")
                    .append(String.join(", ", method.getRequirements()))
                    .append(".");
        }

        return reason.toString();
    }

    private static String pretty(String value)
    {
        String lower = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0))
                + lower.substring(1);
    }
}
