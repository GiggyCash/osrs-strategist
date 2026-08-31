package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Gear acquisition chains loaded from the bundled catalog. */
@Singleton
public class GearAcquisitionCatalog
{
    public static final String PROVENANCE = Text.get(249);
    private final Map<String, GearAcquisitionRoute> routes = new LinkedHashMap<>();

    public GearAcquisitionCatalog()
    {
        for (GearAcquisitionRoute route : BundledCatalogLoader.array(
                Text.get(250), GearAcquisitionRoute[].class))
            if (routes.put(normalize(route.getItemName()), route) != null)
                throw new IllegalStateException(Text.get(1136) + route.getItemName());
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
