package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Audited infrastructure milestones loaded from the bundled catalog. */
@Singleton
public final class InfrastructureMilestoneCatalog
{
    public static final String AUDITED_AT = "2026-08-29";
    public static final List<String> PROVENANCE_URLS = Collections.unmodifiableList(Arrays.asList(
            Text.get(317),
            Text.get(319),
            Text.get(320),
            Text.get(321),
            Text.get(322),
            Text.get(323),
            Text.get(324),
            Text.get(325),
            Text.get(326)));
    private final Map<String, InfrastructureMilestoneDefinition> milestones;

    public InfrastructureMilestoneCatalog()
    {
        Map<String, InfrastructureMilestoneDefinition> values = new LinkedHashMap<>();
        for (InfrastructureMilestoneDefinition value : BundledCatalogLoader.array(
                Text.get(318),
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
