package compass;

import java.util.*;
import javax.inject.Singleton;

/** High-value abilities loaded from the bundled catalog. */
@Singleton
public final class AbilityUnlockCatalog
{
    public static final String PROVENANCE =
            Text.get(129);
    private final Map<String, AbilityUnlockDefinition> definitions = new LinkedHashMap<>();

    public AbilityUnlockCatalog()
    {
        for (AbilityUnlockDefinition value : BundledCatalogLoader.array(
                Text.get(130), AbilityUnlockDefinition[].class))
            if (definitions.put(Names.slug(value.id), value) != null)
                throw new IllegalStateException(Text.get(1107) + value.id);
    }
    public AbilityUnlockDefinition get(String id)
    {
        return id == null ? null : definitions.get(Names.slug(id));
    }
    public List<AbilityUnlockDefinition> all()
    {
        return Collections.unmodifiableList(new ArrayList<>(definitions.values()));
    }
}
