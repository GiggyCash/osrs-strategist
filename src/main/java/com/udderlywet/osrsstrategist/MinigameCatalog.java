package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Minigame definitions loaded from the bundled catalog. */
@Singleton
public class MinigameCatalog
{
    private final List<MinigameDefinition> definitions = Collections.unmodifiableList(Arrays.asList(
            BundledCatalogLoader.array("/content/catalogs/minigames.json", MinigameDefinition[].class)));

    public List<MinigameDefinition> all() { return definitions; }
    public MinigameDefinition byId(String id)
    {
        if (id == null) return null;
        for (MinigameDefinition definition : definitions)
            if (id.equals(definition.getId())) return definition;
        return null;
    }
}
