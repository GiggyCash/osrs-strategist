package com.udderlywet.osrsstrategist;

import java.util.*;

/** Provider of verified non-skill work that may compete with skill training. */
public interface CandidateProvider
{
    String getId();
    List<Recommendation> candidates(StrategyContext context);

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
