package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.gameval.VarbitID;

/**
 * Starter herb/tree run catalog using RuneLite's current farming-region varbits.
 * More patch types can be added without changing the run planner.
 */
@Singleton
public class FarmingRunCatalog
{
    private final List<FarmingRunPatchDefinition> patches = Arrays.asList(
            herb("falador", "Falador", 9, VarbitID.FARMING_TRANSMIT_D, null, 12083),
            herb("catherby", "Catherby", 9, VarbitID.FARMING_TRANSMIT_D, null, 11062),
            herb("ardougne", "Ardougne", 9, VarbitID.FARMING_TRANSMIT_D, null, 10548),
            herb("hosidius", "Hosidius", 9, VarbitID.FARMING_TRANSMIT_D, null, 6967),
            herb("morytania", "Morytania", 9, VarbitID.FARMING_TRANSMIT_D, "Priest in Peril", 14391),
            herb("troll_stronghold", "Troll Stronghold", 9, VarbitID.FARMING_TRANSMIT_A, "My Arm's Big Adventure", 11321),
            herb("weiss", "Weiss", 9, VarbitID.FARMING_TRANSMIT_A, "Making Friends with My Arm", 11325),
            herb("farming_guild", "Farming Guild", 65, VarbitID.FARMING_TRANSMIT_E, null, 4922),

            tree("lumbridge", "Lumbridge", 15, VarbitID.FARMING_TRANSMIT_A, null, 12594),
            tree("taverley", "Taverley", 15, VarbitID.FARMING_TRANSMIT_A, null, 11573, 11829),
            tree("varrock", "Varrock", 15, VarbitID.FARMING_TRANSMIT_A, null, 12854),
            tree("falador", "Falador", 15, VarbitID.FARMING_TRANSMIT_A, null, 11828, 12084),
            tree("gnome_stronghold", "Gnome Stronghold", 15, VarbitID.FARMING_TRANSMIT_A, null, 9781),
            tree("farming_guild", "Farming Guild", 65, VarbitID.FARMING_TRANSMIT_G, null, 4922)
    );

    public List<FarmingRunPatchDefinition> all()
    {
        return Collections.unmodifiableList(patches);
    }

    public List<FarmingRunPatchDefinition> forRegion(int regionId)
    {
        List<FarmingRunPatchDefinition> result = new ArrayList<>();
        for (FarmingRunPatchDefinition patch : patches)
        {
            if (patch.matchesRegion(regionId)) result.add(patch);
        }
        return result;
    }

    private static FarmingRunPatchDefinition herb(
            String id, String name, int level, int varbit,
            String quest, Integer... regions)
    {
        return definition("herb_" + id, name, FarmingPatchKind.HERB,
                level, varbit, quest, regions);
    }

    private static FarmingRunPatchDefinition tree(
            String id, String name, int level, int varbit,
            String quest, Integer... regions)
    {
        return definition("tree_" + id, name, FarmingPatchKind.TREE,
                level, varbit, quest, regions);
    }

    private static FarmingRunPatchDefinition definition(
            String id, String name, FarmingPatchKind kind, int level,
            int varbit, String quest, Integer... regions)
    {
        return new FarmingRunPatchDefinition(
                id, name, kind, level,
                new HashSet<>(Arrays.asList(regions)), varbit, quest);
    }
}
