package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/** Farming run patches loaded from the bundled catalog. */
@Singleton
public class FarmingRunCatalog
{
    private final List<FarmingRunPatchDefinition> patches = Collections.unmodifiableList(Arrays.asList(
            BundledCatalogLoader.array("/content/catalogs/farming-run-patches.json",
                    FarmingRunPatchDefinition[].class)));
    public List<FarmingRunPatchDefinition> all() { return patches; }
    public List<FarmingRunPatchDefinition> forRegion(int regionId)
    {
        List<FarmingRunPatchDefinition> result = new ArrayList<>();
        for (FarmingRunPatchDefinition patch : patches)
            if (patch.matchesRegion(regionId)) result.add(patch);
        return result;
    }
}
