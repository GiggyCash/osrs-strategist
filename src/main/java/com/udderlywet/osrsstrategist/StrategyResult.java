package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

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
    @Getter
    private final List<Recommendation> recommendations;
    @Getter
    private final List<Opportunity> opportunities;
    @Getter
    private final List<StrategySignal> signals;
    @Getter
    private final StrategicPlan plan;

    public StrategyResult(
            List<Recommendation> recommendations,
            List<Opportunity> opportunities)
    {
        this(
                recommendations,
                opportunities,
                Collections.emptyList(),
                null
        );
    }

    public StrategyResult(
            List<Recommendation> recommendations,
            List<Opportunity> opportunities,
            List<StrategySignal> signals)
    {
        this(recommendations, opportunities, signals, null);
    }

    public StrategyResult(
            List<Recommendation> recommendations,
            List<Opportunity> opportunities,
            List<StrategySignal> signals,
            StrategicPlan plan)
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
        this.plan = plan;
    }





    public StrategyResult withPlan(StrategicPlan value)
    {
        return new StrategyResult(recommendations, opportunities, signals,
                value);
    }
}
