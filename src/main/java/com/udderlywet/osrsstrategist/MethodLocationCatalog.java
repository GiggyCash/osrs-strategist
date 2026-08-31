package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Audited route locations loaded from the bundled catalog. */
@Singleton
public final class MethodLocationCatalog
{
    public static final String ECTOFUNTUS_SOURCE = "https://oldschool.runescape.wiki/w/Ectofuntus";
    public static final String FRUIT_TREE_SOURCE = "https://oldschool.runescape.wiki/w/Fruit_tree_patch/Patches";
    public static final String TREE_PATCH_SOURCE = "https://oldschool.runescape.wiki/w/Tree_patch";
    public static final String FLY_FISHING_SOURCE = "https://oldschool.runescape.wiki/w/Free-to-play_Fishing_training";
    private final Map<String, MethodLocationProfile> profiles;

    public MethodLocationCatalog()
    {
        Map<String, MethodLocationProfile> values = new LinkedHashMap<>();
        for (MethodLocationProfile profile : BundledCatalogLoader.array(
                "/content/catalogs/method-locations.json", MethodLocationProfile[].class))
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
