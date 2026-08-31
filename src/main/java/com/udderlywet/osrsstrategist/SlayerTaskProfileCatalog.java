package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Stable task mechanics loaded from the required bundled catalog. */
@Singleton
public class SlayerTaskProfileCatalog
{
    private static final String RESOURCE = Text.get(892);
    private final List<SlayerTaskProfile> profiles;

    public SlayerTaskProfileCatalog()
    {
        profiles = Collections.unmodifiableList(Arrays.asList(
                BundledCatalogLoader.array(RESOURCE, SlayerTaskProfile[].class)));
        for (SlayerTaskProfile profile : profiles)
            if (profile.getId() == null || profile.getAliases() == null
                    || profile.getAliases().isEmpty())
                throw new IllegalStateException(Text.get(1184) + RESOURCE);
    }

    public SlayerTaskProfile profileFor(String taskName)
    {
        var normalized = Names.lower(taskName);
        if (normalized.isEmpty()) return null;
        for (SlayerTaskProfile profile : profiles)
            for (String alias : profile.getAliases())
            {
                var candidate = Names.lower(alias);
                if (normalized.equals(candidate) || normalized.contains(candidate)
                        || candidate.contains(normalized)) return profile;
            }
        return null;
    }

    public List<SlayerTaskProfile> all() { return profiles; }

}
