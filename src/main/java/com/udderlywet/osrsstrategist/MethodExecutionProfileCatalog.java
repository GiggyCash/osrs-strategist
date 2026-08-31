package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Deterministic execution profiles loaded from the required bundled catalog. */
@Singleton
public class MethodExecutionProfileCatalog
{
    private static final String RESOURCE = Text.get(371);
    private final Map<String, MethodProfile> profiles;

    public MethodExecutionProfileCatalog()
    {
        Map<String, MethodProfile> values = new LinkedHashMap<>();
        for (MethodProfile profile
                : BundledCatalogLoader.array(RESOURCE, MethodProfile[].class))
        {
            if (profile.getMethodId() == null || profile.getActionTerms() == null)
                throw new IllegalStateException(Text.get(1224) + RESOURCE);
            if (values.put(profile.getMethodId(), profile) != null)
                throw new IllegalStateException(Text.get(1225)
                        + profile.getMethodId());
        }
        profiles = Collections.unmodifiableMap(values);
    }

    public MethodProfile forMethod(String methodId) { return profiles.get(methodId); }
    public Map<String, MethodProfile> all() { return profiles; }
}
