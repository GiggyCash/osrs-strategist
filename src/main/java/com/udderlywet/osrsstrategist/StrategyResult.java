package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Complete output of one strategy evaluation.
 *
 * <p>The sidebar mainly consumes recommendations and opportunities. Signals
 * hold the deeper reasoning behind those decisions so a Details view, debug
 * tools, and future scoring layers can explain decisions without turning the
 * default sidebar into a wall of text.</p>
 */
public final class StrategyResult
{
    private final List<Recommendation> recommendations;
    private final List<Opportunity> opportunities;
    private final List<StrategySignal> signals;

    public StrategyResult(
            List<Recommendation> recommendations,
            List<Opportunity> opportunities)
    {
        this(
                recommendations,
                opportunities,
                Collections.emptyList()
        );
    }

    public StrategyResult(
            List<Recommendation> recommendations,
            List<Opportunity> opportunities,
            List<StrategySignal> signals)
    {
        this.recommendations = Collections.unmodifiableList(
                new ArrayList<>(recommendations)
        );
        this.opportunities = Collections.unmodifiableList(
                new ArrayList<>(opportunities)
        );
        this.signals = Collections.unmodifiableList(
                new ArrayList<>(signals)
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

    public List<StrategySignal> getSignals()
    {
        return signals;
    }
}
