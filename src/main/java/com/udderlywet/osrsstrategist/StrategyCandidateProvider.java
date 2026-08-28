package com.udderlywet.osrsstrategist;

import java.util.List;
import java.util.Collections;
import java.util.Set;

/** Provider of verified non-skill work that may compete with skill training. */
public interface StrategyCandidateProvider
{
    String getId();
    List<StrategyCandidate> candidates(StrategyContext context);

    /**
     * Generic queue entries owned by this richer workflow. The entries are
     * removed only when this provider actually emits a candidate, so missing
     * live evidence cannot silently erase an otherwise safe fallback.
     */
    default Set<String> supersededCandidateIds()
    {
        return Collections.emptySet();
    }
}
