package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Audited transports loaded from the bundled catalog. */
@Singleton
public final class TransportCatalog
{
    public static final String PROVENANCE =
            "Maintained current-live transport and RuneLite shortcut audit";
    private final Map<String, TransportDefinition> routes = new LinkedHashMap<>();

    public TransportCatalog()
    {
        for (TransportDefinition value : BundledCatalogLoader.array(
                "/content/catalogs/transports.json", TransportDefinition[].class))
            if (routes.put(value.getId(), value) != null)
                throw new IllegalStateException("Duplicate transport " + value.getId());
    }

    public List<TransportDefinition> all()
    {
        return Collections.unmodifiableList(new ArrayList<>(routes.values()));
    }
    public TransportDefinition get(String id) { return routes.get(id); }
}
