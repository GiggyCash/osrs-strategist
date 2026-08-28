package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ordered unfinished quest work derived only from verified dependency edges. */
public final class QuestPathPlan
{
    private final List<QuestPathStep> steps;

    QuestPathPlan(List<QuestPathStep> steps)
    {
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public List<QuestPathStep> getSteps() { return steps; }

    public QuestPathStep nextEligibleStep()
    {
        for (QuestPathStep step : steps)
            if (step.isEligibleNow()) return step;
        return null;
    }

    public QuestPathStep stepForQuest(String questName)
    {
        String expected = normalize(questName);
        for (QuestPathStep step : steps)
            if (normalize(step.getQuestName()).equals(expected)) return step;
        return null;
    }

    public boolean isEmpty() { return steps.isEmpty(); }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(
                java.util.Locale.ROOT).replace('\u2019', '\'')
                .replaceAll("[^a-z0-9]+", " ").trim();
    }
}
