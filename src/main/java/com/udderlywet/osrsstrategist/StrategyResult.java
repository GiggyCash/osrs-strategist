package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StrategyResult
{
    private final List<Recommendation> recommendations;
    private final List<Opportunity> opportunities;

    public StrategyResult(
            List<Recommendation> recommendations,
            List<Opportunity> opportunities)
    {
        this.recommendations = Collections.unmodifiableList(
                new ArrayList<>(recommendations)
        );
        this.opportunities = Collections.unmodifiableList(
                new ArrayList<>(opportunities)
        );
    }

    public List<Recommendation> getRecommendations()
    {
        return recommendations;
    }

    public List<Opportunity> getOpportunities()
    {
        return opportunities;
    }
}
