package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Audited exact routes that can be derived from ordinary live account state. */
@Singleton
public final class TravelRouteEvidenceCatalog
{
    private final Map<String, TravelRouteEvidenceDefinition> definitions =
            new LinkedHashMap<>();

    public TravelRouteEvidenceCatalog()
    {
        add(new TravelRouteEvidenceDefinition(
                "spirit-tree-gnome-stronghold", "The Grand Tree",
                Collections.emptyList()));
        add(new TravelRouteEvidenceDefinition("ectophial", "Ghosts Ahoy",
                Arrays.asList("Ectophial")));
    }

    public TravelRouteEvidenceDefinition get(String id)
    {
        return id == null ? null : definitions.get(id);
    }

    public Map<String, TravelRouteEvidenceDefinition> all()
    {
        return Collections.unmodifiableMap(definitions);
    }

    private void add(TravelRouteEvidenceDefinition definition)
    {
        if (definitions.put(definition.getRouteId(), definition) != null)
            throw new IllegalStateException("Duplicate exact travel route "
                    + definition.getRouteId());
    }
}
