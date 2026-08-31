package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/**
 * Complete output of one strategy evaluation.
 *
 * <p>The sidebar mainly consumes recommendations and opportunities. Signals
 * hold the deeper reasoning behind those decisions so a Details view, debug
 * tools, and future scoring layers can explain decisions without turning the
 * default sidebar into a wall of text.</p>
 */
@Getter
public final class StrategyResult
{
    private final List<Recommendation> recommendations;
    private final List<Opportunity> opportunities;
    private final StrategicPlan plan;

    public StrategyResult(
            List<Recommendation> recommendations,
            List<Opportunity> opportunities)
    {
        this(
                recommendations,
                opportunities,
                null
        );
    }

    public StrategyResult(
            List<Recommendation> recommendations,
            List<Opportunity> opportunities,
            StrategicPlan plan)
    {
        this.recommendations = Collections.unmodifiableList(
                new ArrayList<>(recommendations)
        );
        this.opportunities = Collections.unmodifiableList(
                new ArrayList<>(opportunities)
        );
        this.plan = plan;
    }





    public StrategyResult withPlan(StrategicPlan value)
    {
        return new StrategyResult(recommendations, opportunities, value);
    }
}
