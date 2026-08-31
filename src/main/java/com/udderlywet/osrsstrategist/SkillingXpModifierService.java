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
            GameData data,
            Skill skill,
            boolean useGroupStorage)
    {
        if (data == null || skill == null) return SkillingXpModifier.none();
        ItemIndex items = new ItemIndex(data, useGroupStorage);

        switch (skill)
        {
            case FISHING:
                if (hasAngler(items))
                    return full(Text.get(1208));
                break;
            case MINING:
                if (hasProspector(items))
                    return full(Text.get(1209));
                break;
            case WOODCUTTING:
                if (hasLumberjack(items))
                    return full(Text.get(1210));
                break;
            case FARMING:
                if (hasFarmer(items))
                    return full(Text.get(1211));
                break;
            case FIREMAKING:
                if (hasPyromancer(items))
                    return full(Text.get(1212));
                break;
            case CONSTRUCTION:
                if (hasCarpenter(items))
                    return full(Text.get(1213));
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

    private static boolean hasAngler(ItemIndex items)
    {
        return items.has("Angler hat", Text.get(1214))
                && items.has("Angler top", "Spirit angler top")
                && items.has("Angler waders", Text.get(1215))
                && items.has("Angler boots", Text.get(1216));
    }

    private static boolean hasProspector(ItemIndex items)
    {
        return items.has("Prospector helmet", Text.get(1217))
                && items.has("Prospector jacket", Text.get(1218), "Varrock armour 4")
                && items.has("Prospector legs", Text.get(1219))
                && items.has("Prospector boots", Text.get(1220));
    }

    private static boolean hasLumberjack(ItemIndex items)
    {
        return items.has("Lumberjack hat", "Forestry hat")
                && items.has("Lumberjack top", "Forestry top")
                && items.has("Lumberjack legs", "Forestry legs")
                && items.has("Lumberjack boots", "Forestry boots");
    }

    private static boolean hasFarmer(ItemIndex items)
    {
        return items.has("Farmer's strawhat")
                && items.has("Farmer's jacket", "Farmer's shirt")
                && items.has(Text.get(1221))
                && items.has("Farmer's boots");
    }

    private static boolean hasPyromancer(ItemIndex items)
    {
        return items.has("Pyromancer hood")
                && items.has("Pyromancer garb")
                && items.has("Pyromancer robe")
                && items.has("Pyromancer boots");
    }

    private static boolean hasCarpenter(ItemIndex items)
    {
        return items.has(Text.get(1222))
                && items.has("Carpenter's shirt")
                && items.has(Text.get(1223))
                && items.has("Carpenter's boots");
    }
}
