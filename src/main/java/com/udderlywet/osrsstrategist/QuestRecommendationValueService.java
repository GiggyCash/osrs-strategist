package com.udderlywet.osrsstrategist;

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
        QuestPathPlan plan = planner.plan(context);
        String quest = recommendation.getId().substring("quest:".length());
        QuestPathStep step = plan.stepForQuest(quest.replace('-', ' '));
        return step == null ? recommendation
                : recommendation.withStrategicValue(
                        recommendation.getStrategicValue().merge(
                                step.strategicValue()));
    }
}
