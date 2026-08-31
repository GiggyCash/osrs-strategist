package compass;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.poh.PohIcons;

/**
 * Observes durable furniture in the current character's own POH.
 *
 * <p>Scene objects alone cannot distinguish a personal house from a host's
 * house. RuneLite's building-mode varbit is therefore the ownership boundary:
 * guests cannot enable it. A complete build-mode scene scan can prove both
 * presence and absence, while every other scene returns no observation.</p>
 */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class LivePohStateReader
{
    public static final String ARMOUR_CASE = "poh-armour-case";
    public static final String COSTUME_ROOM = Text.get(1709);
    public static final String PERMANENT_PORTAL = Text.get(1710);
    public static final String PORTAL_NEXUS = Text.get(1711);
    public static final String RESTORATION_POOL = Text.get(1712);
    public static final String SUPERIOR_GARDEN = Text.get(1713);
    public static final String ORNATE_POOL = "poh-ornate-pool";
    public static final String JEWELLERY_BOX = Text.get(1714);
    public static final String ORNATE_JEWELLERY_BOX = Text.get(1715);
    public static final String SPELLBOOK_ALTAR = Text.get(1716);
    public static final String OCCULT_ALTAR = Text.get(1717);
    public static final String FAIRY_RING = "poh-fairy-ring";
    public static final String SPIRIT_TREE = "poh-spirit-tree";
    public static final String SPIRITUAL_FAIRY_TREE = Text.get(1718);
    public static final String MOUNTED_GLORY = Text.get(1719);
    public static final String ARMOUR_STAND = Text.get(1720);

    private static final Set<String> TRACKED = new HashSet<>(Arrays.asList(
            COSTUME_ROOM, ARMOUR_CASE, PERMANENT_PORTAL, PORTAL_NEXUS,
            SUPERIOR_GARDEN, RESTORATION_POOL, ORNATE_POOL, JEWELLERY_BOX,
            ORNATE_JEWELLERY_BOX, SPELLBOOK_ALTAR, OCCULT_ALTAR,
            FAIRY_RING, SPIRIT_TREE, SPIRITUAL_FAIRY_TREE,
            MOUNTED_GLORY, ARMOUR_STAND));

    private final Client client;

    public PohSnapshot read()
    {
        if (client == null || client.getGameState() != GameState.LOGGED_IN
                || client.getVarbitValue(VarbitID.POH_BUILDING_MODE) != 1)
            return null;
        var worldView = client.getTopLevelWorldView();
        var scene = worldView == null ? null : worldView.getScene();
        if (scene == null || scene.getTiles() == null) return null;

        Set<Integer> objectIds = new HashSet<>();
        for (Tile[][] plane : scene.getTiles())
            if (plane != null)
                for (Tile[] column : plane)
                    if (column != null)
                        for (Tile tile : column) collect(tile, objectIds);
        return snapshotForObjectIds(objectIds);
    }

    static PohSnapshot snapshotForObjectIds(Set<Integer> objectIds)
    {
        Map<String, CapabilityState> furniture = new LinkedHashMap<>();
        for (String key : TRACKED) furniture.put(key, CapabilityState.BLOCKED);
        if (objectIds != null)
            for (Integer id : objectIds)
                if (id != null) classify(id, furniture);
        return new PohSnapshot(CapabilityState.VERIFIED, furniture);
    }

    private static void collect(Tile tile, Set<Integer> ids)
    {
        if (tile == null) return;
        add(tile.getDecorativeObject(), ids);
        add(tile.getGroundObject(), ids);
        add(tile.getWallObject(), ids);
        var gameObjects = tile.getGameObjects();
        if (gameObjects != null)
            for (GameObject object : gameObjects) add(object, ids);
    }

    private static void add(TileObject object, Set<Integer> ids)
    {
        if (object != null && object.getId() >= 0) ids.add(object.getId());
    }

    private static void classify(int id, Map<String, CapabilityState> values)
    {
        // Only completed armour cases are in this range; the build hotspot is
        // outside it and is deliberately not treated as storage.
        if (id >= ObjectID.POH_COS_ROOM_CAPE_RACK_OAK
                && id <= ObjectID.POH_COS_ROOM_ARMOUR_CASE_HOTSPOT)
            verify(values, COSTUME_ROOM);
        if (id >= ObjectID.POH_COS_ROOM_ARMOUR_CASE_OAK
                && id <= ObjectID.POH_COS_ROOM_ARMOUR_CASE_OPEN_MAHOGANY)
            verify(values, ARMOUR_CASE);

        var icon = PohIcons.getIcon(id);
        if (icon != null)
        {
            switch (icon)
            {
                case PORTALNEXUS:
                    verify(values, PORTAL_NEXUS);
                    break;
                case POOLS:
                    verify(values, RESTORATION_POOL);
                    break;
                case JEWELLERYBOX:
                    verify(values, JEWELLERY_BOX);
                    break;
                case SPELLBOOKALTAR:
                    verify(values, SPELLBOOK_ALTAR);
                    break;
                case MAGICTRAVEL:
                    if (id == ObjectID.POH_FAIRY_RING)
                        verify(values, FAIRY_RING);
                    else if (id == ObjectID.POH_SPIRIT_TREE)
                        verify(values, SPIRIT_TREE);
                    else if (id == ObjectID.POH_SPIRIT_RING)
                    {
                        verify(values, FAIRY_RING);
                        verify(values, SPIRIT_TREE);
                        verify(values, SPIRITUAL_FAIRY_TREE);
                    }
                    break;
                case GLORY:
                    verify(values, MOUNTED_GLORY);
                    break;
                case REPAIR:
                    verify(values, ARMOUR_STAND);
                    break;
                case EXITPORTAL:
                case ALTAR:
                case XERICSTALISMAN:
                case DIGSITEPENDANT:
                case MYTHICALCAPE:
                    break;
                default:
                    // Every remaining PohIcons entry is a configured portal
                    // destination, not an empty frame or hotspot.
                    verify(values, PERMANENT_PORTAL);
                    break;
            }
        }

        if (id == ObjectID.POH_POOL_REGENERATION)
            verify(values, ORNATE_POOL);
        if ((id >= ObjectID.POH_SUPERIOR_GARDEN_HOTSPOT_TREERING
                && id <= ObjectID.POH_SUPERIOR_GARDEN_HOTSPOT_SEATING_B_RIGHT)
                || (id >= ObjectID.POH_SPIRIT_TREE
                && id <= ObjectID.POH_POOL_REGENERATION))
            verify(values, SUPERIOR_GARDEN);
        if (id == ObjectID.POH_JEWELLERY_BOX_3)
            verify(values, ORNATE_JEWELLERY_BOX);
        if (id == ObjectID.POH_ALTAR_OCCULT
                || id == ObjectID.POH_ALTAR_OCCULT_STANDARD
                || id == ObjectID.POH_ALTAR_OCCULT_ANCIENT
                || id == ObjectID.POH_ALTAR_OCCULT_LUNAR
                || id == ObjectID.POH_ALTAR_OCCULT_ARCEUUS)
            verify(values, OCCULT_ALTAR);
    }

    private static void verify(Map<String, CapabilityState> values, String key)
    {
        values.put(key, CapabilityState.VERIFIED);
    }
}
