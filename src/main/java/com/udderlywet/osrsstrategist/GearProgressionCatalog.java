package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/** Encounter-context gear progression loaded from the bundled catalog. */
@Singleton
public class GearProgressionCatalog
{
    private final List<GearProgressionEntry> entries = Collections.unmodifiableList(Arrays.asList(
            BundledCatalogLoader.array("/content/catalogs/gear-progression.json",
                    GearProgressionEntry[].class)));

    public List<GearProgressionEntry> all() { return entries; }
    public List<GearProgressionEntry> forStyle(CombatStyle style)
    {
        List<GearProgressionEntry> result = new ArrayList<>();
        for (GearProgressionEntry entry : entries)
            if (entry.getStyle() == style) result.add(entry);
        return Collections.unmodifiableList(result);
    }
    public List<GearProgressionEntry> forContext(String contextId)
    {
        List<GearProgressionEntry> result = new ArrayList<>();
        for (GearProgressionEntry entry : entries)
            if (entry.getContextId().equals(contextId)) result.add(entry);
        return Collections.unmodifiableList(result);
    }
}
