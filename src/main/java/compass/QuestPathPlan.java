package compass;

import java.util.*;

import lombok.Getter;

/** Ordered unfinished quest work derived only from verified dependency edges. */
public final class QuestPathPlan
{
    @Getter
    private final List<QuestPathStep> steps;

    QuestPathPlan(List<QuestPathStep> steps)
    {
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }


    public QuestPathStep nextEligibleStep()
    {
        for (QuestPathStep step : steps)
            if (step.isEligibleNow()) return step;
        return null;
    }

    public QuestPathStep stepForQuest(String questName)
    {
        var expected = Names.words(questName);
        for (QuestPathStep step : steps)
            if (Names.words(step.getQuestName()).equals(expected)) return step;
        return null;
    }

    public boolean isEmpty() { return steps.isEmpty(); }

}
