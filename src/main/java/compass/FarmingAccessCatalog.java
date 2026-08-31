package compass;

import java.util.*;
import javax.inject.Singleton;

/** Verified Farming access evidence loaded from the bundled catalog. */
@Singleton
public class FarmingAccessCatalog
{
    private final List<FarmingAccessDefinition> definitions =
            Collections.unmodifiableList(Arrays.asList(BundledCatalogLoader.array(
                    "/content/catalogs/farming-access.json",
                    FarmingAccessDefinition[].class)));

    public List<FarmingAccessDefinition> all()
    {
        return Collections.unmodifiableList(definitions);
    }

    public FarmingAccessDefinition forRegion(int regionId)
    {
        for (FarmingAccessDefinition definition : definitions)
        {
            if (definition.getRegionIds().contains(regionId))
            {
                return definition;
            }
        }
        return null;
    }

}
