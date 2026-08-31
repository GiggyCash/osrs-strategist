package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Audited route locations loaded from the bundled catalog. */
@Singleton
public final class MethodLocationCatalog
{
    public static final String ECTOFUNTUS_SOURCE = Text.get(373);
    public static final String FRUIT_TREE_SOURCE = Text.get(374);
    public static final String TREE_PATCH_SOURCE = Text.get(375);
    public static final String FLY_FISHING_SOURCE = Text.get(376);
    private final Map<String, MethodLocationProfile> profiles;

    public MethodLocationCatalog()
    {
        Map<String, MethodLocationProfile> values = new LinkedHashMap<>();
        for (MethodLocationProfile profile : BundledCatalogLoader.array(
                Text.get(377), MethodLocationProfile[].class))
            if (values.put(profile.getMethodId(), profile) != null)
                throw new IllegalStateException(Text.get(1153)
                        + profile.getMethodId());
        profiles = Collections.unmodifiableMap(values);
    }
    public MethodLocationProfile forMethod(String methodId)
    {
        return methodId == null ? null : profiles.get(methodId);
    }
    public Map<String, MethodLocationProfile> all() { return profiles; }
}
