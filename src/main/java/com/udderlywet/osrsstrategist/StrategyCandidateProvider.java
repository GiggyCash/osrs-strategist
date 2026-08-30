package com.udderlywet.osrsstrategist;

import java.util.List;

/** Provider of verified non-skill work that may compete with skill training. */
public interface StrategyCandidateProvider
{
    String getId();
    List<StrategyCandidate> candidates(StrategyContext context);
}
