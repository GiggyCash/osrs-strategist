package compass;

import java.util.*;
import javax.inject.Singleton;

/** Reviewable PvM preparation evidence loaded from the bundled catalog. */
@Singleton
public class PvmPreparationProfileCatalog
{
    public static final String PROVENANCE =
            Text.get(441);
    private static final String RESOURCE = Text.get(442);
    private final Map<String, PvmPreparationProfile> profiles;

    public PvmPreparationProfileCatalog()
    {
        Map<String, PvmPreparationProfile> values = new LinkedHashMap<>();
        for (PvmPreparationProfile profile
                : BundledCatalogLoader.array(RESOURCE, PvmPreparationProfile[].class))
        {
            if (profile.getActivityId() == null || profile.getChecks() == null)
                throw new IllegalStateException(Text.get(1164) + RESOURCE);
            if (values.put(profile.getActivityId(), profile) != null)
                throw new IllegalStateException(Text.get(1165)
                        + profile.getActivityId());
        }
        profiles = Collections.unmodifiableMap(values);
    }

    public PvmPreparationProfile forActivity(String id) { return profiles.get(id); }
    public Map<String, PvmPreparationProfile> all() { return profiles; }
}
