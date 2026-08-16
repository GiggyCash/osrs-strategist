package com.udderlywet.osrsstrategist;

import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

@Singleton
public class TrainingMethodSelector
{
    private final TrainingMethodDatabase database;

    @Inject
    public TrainingMethodSelector(TrainingMethodDatabase database)
    {
        this.database = database;
    }

    public TrainingPlan select(
            Skill skill,
            int currentLevel,
            StrategyMode strategyMode,
            SessionIntent sessionIntent)
    {
        List<TrainingMethod> methods = database.methodsFor(skill);

        TrainingMethod best = methods.stream()
                .filter(method -> method.supportsLevel(currentLevel))
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

        return new TrainingPlan(
                best,
                buildExplanation(
                        best,
                        strategyMode,
                        sessionIntent
                )
        );
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
