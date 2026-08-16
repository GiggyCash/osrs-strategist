package com.udderlywet.osrsstrategist;

import java.util.List;

/**
 * Contract for a specialized Strategist subsystem.
 *
 * <p>Examples include goals, clues, PvM readiness, account-mode restrictions,
 * economy planning, and collection-log opportunities. Modules produce signals;
 * the top-level engine decides how to combine them.</p>
 */
public interface StrategyModule
{
    String getId();

    List<StrategySignal> analyze(StrategyContext context);
}
