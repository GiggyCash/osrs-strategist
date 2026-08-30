package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.inject.Singleton;

/** Stable task mechanics loaded from the required bundled catalog. */
@Singleton
public class SlayerTaskProfileCatalog
{
    private static final String RESOURCE = "/content/catalogs/slayer-task-profiles.json";
    private final List<SlayerTaskProfile> profiles;

    public SlayerTaskProfileCatalog()
    {
        profiles = Collections.unmodifiableList(Arrays.asList(
                BundledCatalogLoader.array(RESOURCE, SlayerTaskProfile[].class)));
        for (SlayerTaskProfile profile : profiles)
            if (profile.getId() == null || profile.getAliases() == null
                    || profile.getAliases().isEmpty())
                throw new IllegalStateException("Incomplete Slayer profile in " + RESOURCE);
    }

    public SlayerTaskProfile profileFor(String taskName)
    {
        String normalized = normalize(taskName);
        if (normalized.isEmpty()) return null;
        for (SlayerTaskProfile profile : profiles)
            for (String alias : profile.getAliases())
            {
                String candidate = normalize(alias);
                if (normalized.equals(candidate) || normalized.contains(candidate)
                        || candidate.contains(normalized)) return profile;
            }
        return null;
    }

    public List<SlayerTaskProfile> all() { return profiles; }

    private static String normalize(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
