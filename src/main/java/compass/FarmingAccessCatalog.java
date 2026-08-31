package compass;

import javax.inject.Singleton;

/** Verified Farming access evidence loaded from the bundled catalog. */
@Singleton
public class FarmingAccessCatalog extends CatalogStore<FarmingAccessDefinition>
{
    public FarmingAccessCatalog() { super(Text.get(1704), FarmingAccessDefinition[].class); }

    public FarmingAccessDefinition forRegion(int regionId)
    {
        return find(value -> value.getRegionIds().contains(regionId));
    }

}
