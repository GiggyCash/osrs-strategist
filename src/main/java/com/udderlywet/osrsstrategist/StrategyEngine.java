package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Top-level coordinator. As more readers come online, they feed a
 * StrategyDataBundle here while the lower-level engines remain focused.
 */
@Singleton
public class StrategyEngine
{
    private final RecommendationEngine recommendationEngine;
    private final OpportunityEngine opportunityEngine;

    @Inject
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine)
    {
        this.recommendationEngine = recommendationEngine;
        this.opportunityEngine = opportunityEngine;
    }

    public StrategyResult evaluate(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        if (data == null || data.getAccount() == null)
        {
            return new StrategyResult(
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        List<Recommendation> recommendations =
                recommendationEngine.recommend(
                        data.getAccount(),
                        strategyMode,
                        sessionIntent,
                        preferenceProfile
                );

        List<Opportunity> opportunities =
                opportunityEngine.evaluate(
                        data.getAccount()
                );

        return new StrategyResult(
                recommendations,
                opportunities
        );
    }
}
