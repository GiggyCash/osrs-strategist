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
                Text.get(1941),
                Text.get(1172),
                1,
                ItemID.BLANKRUNE,
                ItemID.BLANKRUNE_HIGH
        );
    }

    public ResourceRequirement altarEntryFor(String methodId)
    {
        if (Text.get(1875).equals(methodId))
            return entry("air", Text.get(1173),
                    ItemID.AIR_TALISMAN, ItemID.TIARA_AIR);
        if (Text.get(1876).equals(methodId))
            return entry("mind", Text.get(1174),
                    ItemID.MIND_TALISMAN, ItemID.TIARA_MIND);
        if (Text.get(1877).equals(methodId))
            return entry("water", Text.get(1175),
                    ItemID.WATER_TALISMAN, ItemID.TIARA_WATER);
        if (Text.get(1878).equals(methodId))
            return entry("earth", Text.get(1176),
                    ItemID.EARTH_TALISMAN, ItemID.TIARA_EARTH);
        if (Text.get(1879).equals(methodId))
            return entry("fire", Text.get(1177),
                    ItemID.FIRE_TALISMAN, ItemID.TIARA_FIRE);
        if (Text.get(1880).equals(methodId))
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
                Text.get(1942) + rune + "_entry",
                label,
                1,
                talismanId,
                tiaraId
        );
    }
}
