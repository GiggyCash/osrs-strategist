package com.udderlywet.osrsstrategist;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Reviewed Slayer task strategy loaded from the bundled catalog. */
@Singleton
public class SlayerTaskStrategicCatalog
{
    private static final String RESOURCE = "/content/catalogs/slayer-task-strategies.json";
    private static final Set<String> INTRINSIC_WILDERNESS = ids(
            "black-knights", "dark-warriors", "earth-warriors", "ents", "green-dragons",
            "lava-dragons", "magic-axes", "mammoths", "revenants", "rogues");
    private final SlayerTaskProfileCatalog taskProfiles;
    private final Map<String, SlayerTaskStrategicProfile> byProfileId;

    @Inject
    public SlayerTaskStrategicCatalog(SlayerTaskProfileCatalog taskProfiles)
    {
        this.taskProfiles = taskProfiles == null ? new SlayerTaskProfileCatalog() : taskProfiles;
        Map<String, SlayerTaskStrategicProfile> values = new HashMap<>();
        for (SlayerTaskStrategicProfile profile
                : BundledCatalogLoader.array(RESOURCE, SlayerTaskStrategicProfile[].class))
        {
            if (profile.getTaskProfileId() == null)
                throw new IllegalStateException("Incomplete Slayer strategy in " + RESOURCE);
            if (values.put(profile.getTaskProfileId(), profile) != null)
                throw new IllegalStateException("Duplicate Slayer strategy: "
                        + profile.getTaskProfileId());
        }
        byProfileId = Collections.unmodifiableMap(values);
    }

    public SlayerTaskStrategicCatalog() { this(new SlayerTaskProfileCatalog()); }

    public SlayerTaskStrategicProfile profileFor(String taskName)
    {
        SlayerTaskProfile mechanics = taskProfiles.profileFor(taskName);
        return mechanics == null ? null : byProfileId.get(mechanics.getId());
    }

    public int size() { return byProfileId.size(); }
    public Collection<SlayerTaskStrategicProfile> all() { return byProfileId.values(); }

    public boolean isWildernessBound(String taskName)
    {
        SlayerTaskProfile mechanics = taskProfiles.profileFor(taskName);
        if (mechanics == null) return false;
        SlayerTaskStrategicProfile profile = byProfileId.get(mechanics.getId());
        return INTRINSIC_WILDERNESS.contains(mechanics.getId())
                || (profile != null && profile.isDirectEncounter()
                        && profile.getInherentRisk() == RiskLevel.HIGH);
    }

    private static Set<String> ids(String... values)
    {
        Set<String> result = new HashSet<>();
        Collections.addAll(result, values);
        return Collections.unmodifiableSet(result);
    }
}
