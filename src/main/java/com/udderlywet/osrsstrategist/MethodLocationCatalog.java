package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Audited route locations loaded from the bundled catalog. */
@Singleton
public final class MethodLocationCatalog
{
    public static final String ECTOFUNTUS_SOURCE = PlayerText.get("MLC1");
    public static final String FRUIT_TREE_SOURCE = PlayerText.get("MLC2");
    public static final String TREE_PATCH_SOURCE = PlayerText.get("MLC3");
    public static final String FLY_FISHING_SOURCE = PlayerText.get("MLC4");
    private final Map<String, MethodLocationProfile> profiles;

    public MethodLocationCatalog()
    {
        Map<String, MethodLocationProfile> values = new LinkedHashMap<>();
        for (MethodLocationProfile profile : BundledCatalogLoader.array(
                PlayerText.get("MLC5"), MethodLocationProfile[].class))
            if (values.put(profile.getMethodId(), profile) != null)
                throw new IllegalStateException("Duplicate method location profile "
                        + profile.getMethodId());
        profiles = Collections.unmodifiableMap(values);
    }
    public MethodLocationProfile forMethod(String methodId)
    {
        return methodId == null ? null : profiles.get(methodId);
    }
    public Map<String, MethodLocationProfile> all() { return profiles; }
}
