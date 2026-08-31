package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Gear acquisition chains loaded from the bundled catalog. */
@Singleton
public class GearAcquisitionCatalog
{
    public static final String PROVENANCE = PlayerText.get("GAC1");
    private final Map<String, GearAcquisitionRoute> routes = new LinkedHashMap<>();

    public GearAcquisitionCatalog()
    {
        for (GearAcquisitionRoute route : BundledCatalogLoader.array(
                PlayerText.get("GAC2"), GearAcquisitionRoute[].class))
            if (routes.put(normalize(route.getItemName()), route) != null)
                throw new IllegalStateException("Duplicate gear route " + route.getItemName());
    }

    public List<GearAcquisitionRoute> all()
    {
        return Collections.unmodifiableList(new ArrayList<>(routes.values()));
    }
    public GearAcquisitionRoute forItem(String itemName) { return routes.get(normalize(itemName)); }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }
}
