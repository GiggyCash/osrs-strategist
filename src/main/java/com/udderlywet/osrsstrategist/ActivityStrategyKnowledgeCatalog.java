package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Sourced activity strategy profiles loaded from the bundled catalog. */
@Singleton
public final class ActivityStrategyKnowledgeCatalog
{
    private final List<ActivityStrategyProfile> profiles = Collections.unmodifiableList(Arrays.asList(
            BundledCatalogLoader.array(Text.get(88),
                    ActivityStrategyProfile[].class)));

    public ActivityStrategyProfile profileFor(String candidateId, AccountMode mode)
    {
        if (candidateId == null || mode == null) return null;
        ActivityStrategyProfile best = null;
        for (ActivityStrategyProfile profile : profiles)
        {
            if (!profile.supports(mode)
                    || !candidateId.startsWith(profile.getCandidatePrefix())) continue;
            if (best == null || profile.getCandidatePrefix().length()
                    > best.getCandidatePrefix().length()) best = profile;
        }
        return best;
    }
    public List<ActivityStrategyProfile> all() { return profiles; }
}
