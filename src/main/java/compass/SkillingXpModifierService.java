package compass;
import static compass.Text.get;

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
        var items = new ItemIndex(data, useGroupStorage);

        switch (skill)
        {
            case FISHING:
                if (hasAngler(items))
                    return full(get(1208));
                break;
            case MINING:
                if (hasProspector(items))
                    return full(get(1209));
                break;
            case WOODCUTTING:
                if (hasLumberjack(items))
                    return full(get(1210));
                break;
            case FARMING:
                if (hasFarmer(items))
                    return full(get(1211));
                break;
            case FIREMAKING:
                if (hasPyromancer(items))
                    return full(get(1212));
                break;
            case CONSTRUCTION:
                if (hasCarpenter(items))
                    return full(get(1213));
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
        return items.has("Angler hat", get(1214))
                && items.has("Angler top", get(1856))
                && items.has("Angler waders", get(1215))
                && items.has("Angler boots", get(1216));
    }

    private static boolean hasProspector(ItemIndex items)
    {
        return items.has(get(1979), get(1217))
                && items.has(get(1980), get(1218), get(1981))
                && items.has("Prospector legs", get(1219))
                && items.has(get(1982), get(1220));
    }

    private static boolean hasLumberjack(ItemIndex items)
    {
        return items.has("Lumberjack hat", "Forestry hat")
                && items.has("Lumberjack top", "Forestry top")
                && items.has("Lumberjack legs", "Forestry legs")
                && items.has(get(1983), "Forestry boots");
    }

    private static boolean hasFarmer(ItemIndex items)
    {
        return items.has(get(1984))
                && items.has("Farmer's jacket", "Farmer's shirt")
                && items.has(get(1221))
                && items.has("Farmer's boots");
    }

    private static boolean hasPyromancer(ItemIndex items)
    {
        return items.has("Pyromancer hood")
                && items.has("Pyromancer garb")
                && items.has("Pyromancer robe")
                && items.has(get(1985));
    }

    private static boolean hasCarpenter(ItemIndex items)
    {
        return items.has(get(1222))
                && items.has(get(1986))
                && items.has(get(1223))
                && items.has(get(1987));
    }
}
