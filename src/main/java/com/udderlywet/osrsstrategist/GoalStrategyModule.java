package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

/**
 * Describes why the currently selected long-term goal matters to the engine.
 *
 * <p>The first version emits structural signals only. Later, individual goal
 * nodes will carry typed requirements so the recommendation engine can score
 * exact dependencies such as quests, skills, gear, and resources.</p>
 */
@Singleton
public class GoalStrategyModule implements StrategyModule
{
    private final GoalGraph goalGraph = new GoalGraph();

    @Override
    public String getId()
    {
        return "goal";
    }

    @Override
    public List<StrategySignal> analyze(StrategyContext context)
    {
        List<StrategySignal> signals = new ArrayList<>();

        GoalType goal = context.getActiveGoal();
        List<String> dependencies = goalGraph.dependenciesFor(goal);

        if (!dependencies.isEmpty())
        {
            signals.add(
                    new StrategySignal(
                            "goal:" + goal.name().toLowerCase(),
                            StrategySignalCategory.GOAL,
                            "Active goal: " + pretty(goal.name())
                                    + " (" + dependencies.size()
                                    + " dependency groups)",
                            0.0,
                            RecommendationConfidence.CHECK_NEEDED
                    )
            );
        }

        return signals;
    }

    private static String pretty(String value)
    {
        String lower = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0))
                + lower.substring(1);
    }
}
