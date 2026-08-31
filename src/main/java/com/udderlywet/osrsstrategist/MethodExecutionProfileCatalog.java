package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Deterministic execution profiles loaded from the required bundled catalog. */
@Singleton
public class MethodExecutionProfileCatalog
{
    private static final String RESOURCE = Text.get(371);
    private final Map<String, MethodExecutionProfile> profiles;

    public MethodExecutionProfileCatalog()
    {
        Map<String, MethodExecutionProfile> values = new LinkedHashMap<>();
        for (MethodExecutionProfile profile
                : BundledCatalogLoader.array(RESOURCE, MethodExecutionProfile[].class))
        {
            if (profile.getMethodId() == null || profile.getActionTerms() == null)
                throw new IllegalStateException("Incomplete execution profile in " + RESOURCE);
            if (values.put(profile.getMethodId(), profile) != null)
                throw new IllegalStateException("Duplicate execution profile: "
                        + profile.getMethodId());
        }
        profiles = Collections.unmodifiableMap(values);
    }

    public MethodExecutionProfile forMethod(String methodId) { return profiles.get(methodId); }
    public Map<String, MethodExecutionProfile> all() { return profiles; }
}
