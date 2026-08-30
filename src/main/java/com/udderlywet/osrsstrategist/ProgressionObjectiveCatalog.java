package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/** Persistent method objectives loaded from the bundled catalog. */
@Singleton
public class ProgressionObjectiveCatalog
{
    private final List<ProgressionObjectiveDefinition> objectives =
            Collections.unmodifiableList(Arrays.asList(BundledCatalogLoader.array(
                    "/content/catalogs/progression-objectives.json",
                    ProgressionObjectiveDefinition[].class)));
    public List<ProgressionObjectiveDefinition> all() { return objectives; }
    public ProgressionObjectiveDefinition forMethod(String methodId)
    {
        if (methodId == null) return null;
        for (ProgressionObjectiveDefinition objective : objectives)
            if (methodId.equals(objective.getMethodId())) return objective;
        return null;
    }
}
