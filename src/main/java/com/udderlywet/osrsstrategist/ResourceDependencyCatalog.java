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
                Text.get(598),
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
                    throw new IllegalStateException(Text.get(1170) + value.getItemId());
                var name = Names.words(value.getItemName());
                if (!name.isEmpty()) byName.put(name, value);
            }
        definitions = Collections.unmodifiableMap(byId);
        definitionsByName = Collections.unmodifiableMap(byName);
    }

    public ResourceDependencyDefinition forItem(int itemId) { return definitions.get(itemId); }
    public ResourceDependencyDefinition forItemName(String itemName)
    {
        return definitionsByName.get(Names.words(itemName));
    }
    public int size() { return definitions.size(); }

}
