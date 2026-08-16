package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Top-level strategist coordinator.
 *
 * <p>Everything that can influence a recommendation eventually enters through
 * this class. Skill training stays in {@link RecommendationEngine}; recurring
 * activities stay in {@link OpportunityEngine}; specialized systems contribute
 * structured {@link StrategySignal}s through {@link StrategyModule}s.</p>
 *
 * <p>This prevents the plugin from becoming one enormous switch statement as
 * quests, PvM, clues, diaries, economy, storage, Sailing, and new Jagex content
 * are added.</p>
 */
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

    /**
     * Compatibility entry point for early tests and callers.
     */
    public StrategyResult evaluate(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        return evaluate(
                data,
                strategyMode,
                sessionIntent,
                QuestTolerance.NORMAL,
                GoalType.MAX,
                true,
                false,
                preferenceProfile
        );
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
        if (data == null || data.getAccount() == null)
        {
            return new StrategyResult(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        StrategyContext context = new StrategyContext(
                data,
                strategyMode,
                sessionIntent,
                questTolerance,
                activeGoal,
                useGroupStorage,
                collectionistMode,
                preferenceProfile
        );

        List<Recommendation> recommendations =
                recommendationEngine.recommend(
                        data.getAccount(),
                        context.getStrategyMode(),
                        context.getSessionIntent(),
                        context.getPreferenceProfile()
                );

        List<Opportunity> opportunities =
                opportunityEngine.evaluate(data);

        List<StrategySignal> signals = new ArrayList<>();

        for (StrategyModule module : moduleRegistry.getModules())
        {
            List<StrategySignal> moduleSignals =
                    module.analyze(context);

            if (moduleSignals != null)
            {
                signals.addAll(moduleSignals);
            }
        }

        signals.sort(
                Comparator.comparingDouble(
                        StrategySignal::getScoreDelta
                ).reversed()
        );

        return new StrategyResult(
                recommendations,
                opportunities,
                signals
        );
    }
}
