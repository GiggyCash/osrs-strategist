package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Reviewable PvM preparation evidence loaded from the bundled catalog. */
@Singleton
public class PvmPreparationProfileCatalog
{
    public static final String PROVENANCE =
            "Maintained PvM preparation review; readiness remains evidence-gated";
    private static final String RESOURCE = "/content/catalogs/pvm-preparation-profiles.json";
    private final Map<String, PvmPreparationProfile> profiles;

    public PvmPreparationProfileCatalog()
    {
        Map<String, PvmPreparationProfile> values = new LinkedHashMap<>();
        for (PvmPreparationProfile profile
                : BundledCatalogLoader.array(RESOURCE, PvmPreparationProfile[].class))
        {
            if (profile.getActivityId() == null || profile.getChecks() == null)
                throw new IllegalStateException("Incomplete PvM profile in " + RESOURCE);
            if (values.put(profile.getActivityId(), profile) != null)
                throw new IllegalStateException("Duplicate PvM profile: "
                        + profile.getActivityId());
        }
        profiles = Collections.unmodifiableMap(values);
    }

    public PvmPreparationProfile forActivity(String id) { return profiles.get(id); }
    public Map<String, PvmPreparationProfile> all() { return profiles; }
}
