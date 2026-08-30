package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import lombok.Getter;

/** Machine-readable completeness and safety census for the offline STASH data. */
public final class StashUnitCensus
{
    private final Map<ClueTier, Integer> byTier = new EnumMap<>(ClueTier.class);
    @Getter
    private final int total;
    @Getter
    private final int missingEvidence;
    @Getter
    private final int wildernessUnits;

    public StashUnitCensus()
    {
        int missing = 0;
        int wilderness = 0;
        StashUnitCatalog catalog = new StashUnitCatalog();
        for (StashUnitDefinition unit : catalog.all())
        {
            ClueTier tier = unit.getTier().getClueTier();
            byTier.put(tier, byTier.getOrDefault(tier, 0) + 1);
            if (unit.getLocation().trim().isEmpty()
                    || unit.getClueText().trim().isEmpty()
                    || unit.getStoredEquipmentEvidence().trim().isEmpty()
                    || unit.getWorldPoints().length == 0)
                missing++;
            if (unit.isWilderness()) wilderness++;
        }
        total = catalog.all().size();
        missingEvidence = missing;
        wildernessUnits = wilderness;
    }

    public Map<ClueTier, Integer> getByTier()
    {
        return Collections.unmodifiableMap(byTier);
    }
}
