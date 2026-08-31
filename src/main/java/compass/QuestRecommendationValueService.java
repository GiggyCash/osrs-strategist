package compass;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Adds dependency fan-out value from the typed selected-goal quest plan. */
@Singleton
public final class QuestRecommendationValueService
{
    private final QuestPathPlanningService planner;

    @Inject
    public QuestRecommendationValueService(QuestPathPlanningService planner)
    {
        this.planner = planner == null
                ? new QuestPathPlanningService() : planner;
    }

    public QuestRecommendationValueService()
    {
        this(new QuestPathPlanningService());
    }

    public Recommendation attach(
            Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null || recommendation.getId() == null
                || !recommendation.getId().startsWith("quest:")
                || context == null) return recommendation;
        var plan = planner.plan(context);
        var quest = recommendation.getId().substring("quest:".length());
        var step = plan.stepForQuest(quest.replace('-', ' '));
        return step == null ? recommendation
                : recommendation.withStrategicValue(
                        recommendation.getStrategicValue().merge(
                                step.strategicValue()));
    }
}
