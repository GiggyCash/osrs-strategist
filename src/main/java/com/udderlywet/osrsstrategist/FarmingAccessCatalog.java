package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/**
 * Verified starter catalog for Farming access reasoning.
 *
 * <p>The region IDs mirror RuneLite's current FarmingWorld definitions. The
 * catalog deliberately starts with common allotment/herb locations and the
 * major quest-gated herb patches. We can expand it as more game data is synced.</p>
 */
@Singleton
public class FarmingAccessCatalog
{
    private final List<FarmingAccessDefinition> definitions = Arrays.asList(
            definition("falador", "Falador patches",
                    regions(12083), null, true),
            definition("catherby", "Catherby patches",
                    regions(11062), null, true),
            definition("ardougne", "Ardougne patches",
                    regions(10548), null, true),
            definition("hosidius", "Hosidius patches",
                    regions(6967, 6711), null, true),
            definition("morytania", "Morytania patches",
                    regions(14391), "Priest in Peril", true),
            definition("troll_stronghold", "Troll Stronghold herb patch",
                    regions(11321), "My Arm's Big Adventure", true),
            definition("weiss", "Weiss herb patch",
                    regions(11325), "Making Friends with My Arm", true)
    );

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

    private static FarmingAccessDefinition definition(
            String id,
            String name,
            HashSet<Integer> regionIds,
            String quest,
            boolean herb)
    {
        return new FarmingAccessDefinition(
                id,
                name,
                regionIds,
                quest,
                herb
        );
    }

    private static HashSet<Integer> regions(Integer... ids)
    {
        return new HashSet<>(Arrays.asList(ids));
    }
}
