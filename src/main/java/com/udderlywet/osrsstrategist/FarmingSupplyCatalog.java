package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.gameval.ItemID;

/** Level-aware Farming resource definitions. */
@Singleton
public class FarmingSupplyCatalog
{
    private static final SupplyOption[] HERB_SEEDS = {
            option(9, ItemID.GUAM_SEED),
            option(14, ItemID.MARRENTILL_SEED),
            option(19, ItemID.TARROMIN_SEED),
            option(26, ItemID.HARRALANDER_SEED),
            option(32, ItemID.RANARR_SEED),
            option(38, ItemID.TOADFLAX_SEED),
            option(44, ItemID.IRIT_SEED),
            option(50, ItemID.AVANTOE_SEED),
            option(56, ItemID.KWUARM_SEED),
            option(62, ItemID.SNAPDRAGON_SEED),
            option(67, ItemID.CADANTINE_SEED),
            option(73, ItemID.LANTADYME_SEED),
            option(79, ItemID.DWARF_WEED_SEED),
            option(85, ItemID.TORSTOL_SEED)
    };

    private static final SupplyOption[] TREE_SAPLINGS = {
            option(15, ItemID.PLANTPOT_OAK_SAPLING),
            option(30, ItemID.PLANTPOT_WILLOW_SAPLING),
            option(45, ItemID.PLANTPOT_MAPLE_SAPLING),
            option(60, ItemID.PLANTPOT_YEW_SAPLING),
            option(75, ItemID.PLANTPOT_MAGIC_TREE_SAPLING)
    };

    public ResourceRequirement rake()
    {
        return new ResourceRequirement("resource:rake", "Rake", 1, ItemID.RAKE);
    }

    public ResourceRequirement dibber()
    {
        return new ResourceRequirement("resource:dibber", "Seed dibber", 1, ItemID.DIBBER);
    }

    public ResourceRequirement spade()
    {
        return new ResourceRequirement("resource:spade", "Spade", 1, ItemID.SPADE);
    }

    public ResourceRequirement herbSeedsForLevel(int level)
    {
        return new ResourceRequirement(
                "resource:herb_seeds", "Usable herb seed", 1,
                unlockedItemIds(HERB_SEEDS, level));
    }

    public ResourceRequirement potatoSeeds()
    {
        return new ResourceRequirement(
                "resource:potato_seeds", Text.get(1133), 3,
                ItemID.POTATO_SEED);
    }

    public ResourceRequirement watermelonSeeds()
    {
        return new ResourceRequirement(
                "resource:watermelon_seeds", Text.get(1134), 3,
                ItemID.WATERMELON_SEED);
    }

    public ResourceRequirement treeSaplingsForLevel(int level)
    {
        return new ResourceRequirement(
                "resource:tree_saplings", Text.get(1135), 1,
                unlockedItemIds(TREE_SAPLINGS, level));
    }

    private static int[] unlockedItemIds(SupplyOption[] options, int level)
    {
        List<Integer> ids = new ArrayList<>();
        for (SupplyOption option : options)
        {
            if (level >= option.level) ids.add(option.itemId);
        }
        var result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) result[i] = ids.get(i);
        return result;
    }

    private static SupplyOption option(int level, int itemId)
    {
        return new SupplyOption(level, itemId);
    }

    private static final class SupplyOption
    {
        private final int level;
        private final int itemId;
        private SupplyOption(int level, int itemId)
        {
            this.level = level;
            this.itemId = itemId;
        }
    }
}
