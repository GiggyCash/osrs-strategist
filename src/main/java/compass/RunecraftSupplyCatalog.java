package compass;

import javax.inject.Singleton;
import net.runelite.api.gameval.ItemID;

/**
 * Concrete resource definitions for conventional F2P Runecraft routes.
 *
 * <p>Keeping item IDs here lets the generic readiness engine prove supplies
 * from equipment, inventory, an observed bank, or safe verified UIM storage
 * instead of leaving Runecraft requirements as permanent free-text questions.</p>
 */
@Singleton
public class RunecraftSupplyCatalog
{
    public ResourceRequirement runeEssence()
    {
        return new ResourceRequirement(
                "resource:runecraft_essence",
                Text.get(1172),
                1,
                ItemID.BLANKRUNE,
                ItemID.BLANKRUNE_HIGH
        );
    }

    public ResourceRequirement altarEntryFor(String methodId)
    {
        if ("runecraft_f2p_air".equals(methodId))
            return entry("air", Text.get(1173),
                    ItemID.AIR_TALISMAN, ItemID.TIARA_AIR);
        if ("runecraft_f2p_mind".equals(methodId))
            return entry("mind", Text.get(1174),
                    ItemID.MIND_TALISMAN, ItemID.TIARA_MIND);
        if ("runecraft_f2p_water".equals(methodId))
            return entry("water", Text.get(1175),
                    ItemID.WATER_TALISMAN, ItemID.TIARA_WATER);
        if ("runecraft_f2p_earth".equals(methodId))
            return entry("earth", Text.get(1176),
                    ItemID.EARTH_TALISMAN, ItemID.TIARA_EARTH);
        if ("runecraft_f2p_fire".equals(methodId))
            return entry("fire", Text.get(1177),
                    ItemID.FIRE_TALISMAN, ItemID.TIARA_FIRE);
        if ("runecraft_f2p_body".equals(methodId))
            return entry("body", Text.get(1178),
                    ItemID.BODY_TALISMAN, ItemID.TIARA_BODY);
        return null;
    }

    public boolean supports(String methodId)
    {
        return altarEntryFor(methodId) != null;
    }

    private static ResourceRequirement entry(
            String rune,
            String label,
            int talismanId,
            int tiaraId)
    {
        return new ResourceRequirement(
                "resource:runecraft_" + rune + "_entry",
                label,
                1,
                talismanId,
                tiaraId
        );
    }
}
