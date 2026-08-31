package com.udderlywet.osrsstrategist;

import java.util.*;

/** Exact minigame setup profiles loaded from the bundled catalog. */
public final class MinigameSetupCatalog
{
    private final Map<String, MinigameSetupProfile> profiles;

    public MinigameSetupCatalog()
    {
        Map<String, MinigameSetupProfile> values = new LinkedHashMap<>();
        for (MinigameSetupProfile profile : BundledCatalogLoader.array(
                Text.get(383), MinigameSetupProfile[].class))
            if (values.put(profile.getActivityId(), profile) != null)
                throw new IllegalStateException(Text.get(1154) + profile.getActivityId());
        profiles = Collections.unmodifiableMap(values);
    }

    public MinigameSetupProfile forActivity(String id) { return id == null ? null : profiles.get(id); }
    public int size() { return profiles.size(); }
}
