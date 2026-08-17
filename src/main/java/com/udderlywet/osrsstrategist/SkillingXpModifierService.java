package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Detects full XP-boosting skilling outfits from observed item state.
 *
 * <p>Only full sets are modeled here. Partial sets do give smaller bonuses, but
 * exact piece-by-piece modeling can be layered in later. Full sets are useful
 * now because the common outfits share a verified 2.5% total bonus and the
 * player can simply equip the observed set before following the recommendation.</p>
 */
@Singleton
public class SkillingXpModifierService
{
    private static final double FULL_SET_MULTIPLIER = 1.025;

    public SkillingXpModifier modifier(
            StrategyDataBundle data,
            Skill skill,
            boolean useGroupStorage)
    {
        if (data == null || skill == null) return SkillingXpModifier.none();
        ObservedItemIndex items = new ObservedItemIndex(data, useGroupStorage);

        switch (skill)
        {
            case FISHING:
                if (hasAngler(items))
                    return full("full Angler/Spirit Angler outfit");
                break;
            case MINING:
                if (hasProspector(items))
                    return full("full Prospector outfit");
                break;
            case WOODCUTTING:
                if (hasLumberjack(items))
                    return full("full Lumberjack/Forestry outfit");
                break;
            case FARMING:
                if (hasFarmer(items))
                    return full("full Farmer's outfit");
                break;
            case FIREMAKING:
                if (hasPyromancer(items))
                    return full("full Pyromancer outfit");
                break;
            case CONSTRUCTION:
                if (hasCarpenter(items))
                    return full("full Carpenter's outfit");
                break;
            default:
                break;
        }
        return SkillingXpModifier.none();
    }

    private static SkillingXpModifier full(String label)
    {
        return new SkillingXpModifier(FULL_SET_MULTIPLIER, label);
    }

    private static boolean hasAngler(ObservedItemIndex items)
    {
        return items.has("Angler hat", "Spirit angler headband")
                && items.has("Angler top", "Spirit angler top")
                && items.has("Angler waders", "Spirit angler waders")
                && items.has("Angler boots", "Spirit angler boots");
    }

    private static boolean hasProspector(ObservedItemIndex items)
    {
        return items.has("Prospector helmet", "Golden prospector helmet")
                && items.has("Prospector jacket", "Golden prospector jacket", "Varrock armour 4")
                && items.has("Prospector legs", "Golden prospector legs")
                && items.has("Prospector boots", "Golden prospector boots");
    }

    private static boolean hasLumberjack(ObservedItemIndex items)
    {
        return items.has("Lumberjack hat", "Forestry hat")
                && items.has("Lumberjack top", "Forestry top")
                && items.has("Lumberjack legs", "Forestry legs")
                && items.has("Lumberjack boots", "Forestry boots");
    }

    private static boolean hasFarmer(ObservedItemIndex items)
    {
        return items.has("Farmer's strawhat")
                && items.has("Farmer's jacket", "Farmer's shirt")
                && items.has("Farmer's boro trousers")
                && items.has("Farmer's boots");
    }

    private static boolean hasPyromancer(ObservedItemIndex items)
    {
        return items.has("Pyromancer hood")
                && items.has("Pyromancer garb")
                && items.has("Pyromancer robe")
                && items.has("Pyromancer boots");
    }

    private static boolean hasCarpenter(ObservedItemIndex items)
    {
        return items.has("Carpenter's helmet")
                && items.has("Carpenter's shirt")
                && items.has("Carpenter's trousers")
                && items.has("Carpenter's boots");
    }
}
