package compass;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Reviewed Slayer task strategy loaded from the bundled catalog. */
@Singleton
public class SlayerTaskStrategicCatalog
{
    private static final String RESOURCE = Text.get(893);
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
                throw new IllegalStateException(Text.get(1185) + RESOURCE);
            if (values.put(profile.getTaskProfileId(), profile) != null)
                throw new IllegalStateException(Text.get(1186)
                        + profile.getTaskProfileId());
        }
        byProfileId = Collections.unmodifiableMap(values);
    }

    public SlayerTaskStrategicCatalog() { this(new SlayerTaskProfileCatalog()); }

    public SlayerTaskStrategicProfile profileFor(String taskName)
    {
        var mechanics = taskProfiles.profileFor(taskName);
        return mechanics == null ? null : byProfileId.get(mechanics.id);
    }

    public int size() { return byProfileId.size(); }
    public Collection<SlayerTaskStrategicProfile> all() { return byProfileId.values(); }

    public boolean isWildernessBound(String taskName)
    {
        var mechanics = taskProfiles.profileFor(taskName);
        if (mechanics == null) return false;
        var profile = byProfileId.get(mechanics.id);
        return INTRINSIC_WILDERNESS.contains(mechanics.id)
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
