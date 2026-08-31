package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Audited infrastructure milestones loaded from the bundled catalog. */
@Singleton
public final class InfrastructureMilestoneCatalog
{
    public static final String AUDITED_AT = "2026-08-29";
    public static final List<String> PROVENANCE_URLS = Collections.unmodifiableList(Arrays.asList(
            "https://oldschool.runescape.wiki/w/Construction",
            "https://oldschool.runescape.wiki/w/Costume_room",
            "https://oldschool.runescape.wiki/w/Portal_chamber",
            "https://oldschool.runescape.wiki/w/Portal_nexus",
            "https://oldschool.runescape.wiki/w/Pool_space",
            "https://oldschool.runescape.wiki/w/Achievement_gallery",
            "https://oldschool.runescape.wiki/w/Fairy_rings",
            "https://oldschool.runescape.wiki/w/Spirit_tree",
            "https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Item_Management"));
    private final Map<String, InfrastructureMilestoneDefinition> milestones;

    public InfrastructureMilestoneCatalog()
    {
        Map<String, InfrastructureMilestoneDefinition> values = new LinkedHashMap<>();
        for (InfrastructureMilestoneDefinition value : BundledCatalogLoader.array(
                "/content/catalogs/infrastructure-milestones.json",
                InfrastructureMilestoneDefinition[].class))
            if (values.put(value.getId(), value) != null)
                throw new IllegalStateException("Duplicate infrastructure milestone " + value.getId());
        milestones = Collections.unmodifiableMap(values);
    }

    public InfrastructureMilestoneDefinition get(String id)
    {
        return id == null ? null : milestones.get(id);
    }

    public List<InfrastructureMilestoneDefinition> all()
    {
        return Collections.unmodifiableList(new ArrayList<>(milestones.values()));
    }
}
