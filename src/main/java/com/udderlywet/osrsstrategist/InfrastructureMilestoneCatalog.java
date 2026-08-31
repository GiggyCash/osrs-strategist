package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Audited infrastructure milestones loaded from the bundled catalog. */
@Singleton
public final class InfrastructureMilestoneCatalog
{
    public static final String AUDITED_AT = "2026-08-29";
    public static final List<String> PROVENANCE_URLS = Collections.unmodifiableList(Arrays.asList(
            PlayerText.get("IMC1"),
            PlayerText.get("IMC2"),
            PlayerText.get("IMC3"),
            PlayerText.get("IMC4"),
            PlayerText.get("IMC5"),
            PlayerText.get("IMC6"),
            PlayerText.get("IMC7"),
            PlayerText.get("IMC8"),
            PlayerText.get("IMC9")));
    private final Map<String, InfrastructureMilestoneDefinition> milestones;

    public InfrastructureMilestoneCatalog()
    {
        Map<String, InfrastructureMilestoneDefinition> values = new LinkedHashMap<>();
        for (InfrastructureMilestoneDefinition value : BundledCatalogLoader.array(
                PlayerText.get("IMC10"),
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
