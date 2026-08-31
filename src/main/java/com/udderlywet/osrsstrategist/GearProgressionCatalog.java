package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Encounter-context gear progression loaded from the bundled catalog. */
@Singleton
public class GearProgressionCatalog
{
    private final List<GearProgressionEntry> entries = Collections.unmodifiableList(Arrays.asList(
            BundledCatalogLoader.array(PlayerText.get("GPC1"),
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
