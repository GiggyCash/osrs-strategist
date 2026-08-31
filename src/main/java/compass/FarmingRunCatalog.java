package compass;

import java.util.*;
import javax.inject.Singleton;

/** Farming run patches loaded from the bundled catalog. */
@Singleton
public class FarmingRunCatalog extends CatalogStore<FarmingRunPatchDefinition>
{
    public FarmingRunCatalog() { super(Text.get(218), FarmingRunPatchDefinition[].class); }
    public List<FarmingRunPatchDefinition> forRegion(int regionId)
    {
        return filter(patch -> patch.matchesRegion(regionId));
    }
}
