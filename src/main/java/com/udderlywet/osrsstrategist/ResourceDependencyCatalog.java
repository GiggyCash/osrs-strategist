package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Deterministic recipe graph loaded from the bundled catalog. */
@Singleton
public class ResourceDependencyCatalog
{
    private final Map<Integer, ResourceDependencyDefinition> definitions;
    private final Map<String, ResourceDependencyDefinition> definitionsByName;

    public ResourceDependencyCatalog()
    {
        this(java.util.Arrays.asList(BundledCatalogLoader.array(
                "/content/catalogs/resource-dependencies.json",
                ResourceDependencyDefinition[].class)));
    }

    ResourceDependencyCatalog(List<ResourceDependencyDefinition> values)
    {
        Map<Integer, ResourceDependencyDefinition> byId = new LinkedHashMap<>();
        Map<String, ResourceDependencyDefinition> byName = new LinkedHashMap<>();
        if (values != null)
            for (ResourceDependencyDefinition value : values)
            {
                if (value == null) continue;
                if (byId.put(value.getItemId(), value) != null)
                    throw new IllegalStateException("Duplicate resource dependency " + value.getItemId());
                String name = normalize(value.getItemName());
                if (!name.isEmpty()) byName.put(name, value);
            }
        definitions = Collections.unmodifiableMap(byId);
        definitionsByName = Collections.unmodifiableMap(byName);
    }

    public ResourceDependencyDefinition forItem(int itemId) { return definitions.get(itemId); }
    public ResourceDependencyDefinition forItemName(String itemName)
    {
        return definitionsByName.get(normalize(itemName));
    }
    public int size() { return definitions.size(); }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('’', '\'').replaceAll("[^a-z0-9]+", " ").trim();
    }
}
