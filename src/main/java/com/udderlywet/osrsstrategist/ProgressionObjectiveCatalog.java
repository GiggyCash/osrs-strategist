package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Persistent method objectives loaded from the bundled catalog. */
@Singleton
public class ProgressionObjectiveCatalog
{
    private final List<ProgressionObjectiveDefinition> objectives =
            Collections.unmodifiableList(Arrays.asList(BundledCatalogLoader.array(
                    Text.get(440),
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
