package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StrategyEngine
{
    private final RecommendationEngine recommendationEngine;
    private final OpportunityEngine opportunityEngine;
    private final StrategyModuleRegistry moduleRegistry;

    @Inject
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyModuleRegistry moduleRegistry)
    {
        this.recommendationEngine = recommendationEngine;
        this.opportunityEngine = opportunityEngine;
        this.moduleRegistry = moduleRegistry;
    }

    public StrategyResult evaluate(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        return evaluate(data, strategyMode, sessionIntent, QuestTolerance.NORMAL,
                GoalType.MAX, true, false, false, preferenceProfile);
    }

    public StrategyResult evaluate(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            PreferenceProfile preferenceProfile)
    {
        return evaluate(data, strategyMode, sessionIntent, questTolerance,
                activeGoal, useGroupStorage, collectionistMode, false,
                preferenceProfile);
    }

    public StrategyResult evaluate(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile)
    {
        if (data == null || data.getAccount() == null)
        {
            return new StrategyResult(Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList());
        }

        StrategyContext context = new StrategyContext(
                data, strategyMode, sessionIntent, questTolerance, activeGoal,
                useGroupStorage, collectionistMode, allowWildernessMethods,
                preferenceProfile);

        List<Recommendation> recommendations = recommendationEngine.recommend(
                data, context.getStrategyMode(), context.getSessionIntent(),
                context.isAllowWildernessMethods(), context.getPreferenceProfile());
        List<Opportunity> opportunities = opportunityEngine.evaluate(data);
        List<StrategySignal> signals = new ArrayList<>();
        for (StrategyModule module : moduleRegistry.getModules())
        {
            List<StrategySignal> moduleSignals = module.analyze(context);
            if (moduleSignals != null) signals.addAll(moduleSignals);
        }
        signals.sort(Comparator.comparingDouble(StrategySignal::getScoreDelta).reversed());
        return new StrategyResult(recommendations, opportunities, signals);
    }
}
