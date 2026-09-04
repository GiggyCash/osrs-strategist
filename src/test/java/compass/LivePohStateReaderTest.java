package compass;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

public class LivePohStateReaderTest
{
    @Test
    public void completeOwnHouseScanProvesTrackedAbsence()
    {
        PohSnapshot snapshot = LivePohStateReader.snapshotForObjectIds(
                Collections.emptySet());

        assertEquals(Capability.VERIFIED, snapshot.getHouseAccess());
        assertEquals(Capability.BLOCKED, snapshot.furnitureState(
                LivePohStateReader.ARMOUR_CASE));
        assertEquals(Capability.BLOCKED, snapshot.furnitureState(
                LivePohStateReader.ORNATE_POOL));
    }

    @Test
    public void hotspotProvesRoomButNotStorageFurniture()
    {
        PohSnapshot snapshot = LivePohStateReader.snapshotForObjectIds(
                Collections.singleton(
                        ObjectID.POH_COS_ROOM_ARMOUR_CASE_HOTSPOT));

        assertEquals(Capability.VERIFIED, snapshot.furnitureState(
                LivePohStateReader.COSTUME_ROOM));
        assertEquals(Capability.BLOCKED, snapshot.furnitureState(
                LivePohStateReader.ARMOUR_CASE));
    }

    @Test
    public void configuredObjectsProveOnlyTheirActualCapabilities()
    {
        PohSnapshot snapshot = LivePohStateReader.snapshotForObjectIds(
                new HashSet<>(Arrays.asList(
                        ObjectID.POH_COS_ROOM_ARMOUR_CASE_OAK,
                        ObjectID.POH_PORTAL_TEAK_VARROCK,
                        ObjectID.POH_POOL_REGENERATION,
                        ObjectID.POH_JEWELLERY_BOX_3,
                        ObjectID.POH_ALTAR_OCCULT_ANCIENT,
                        ObjectID.POH_SPIRIT_RING)));

        assertEquals(Capability.VERIFIED, snapshot.furnitureState(
                LivePohStateReader.ARMOUR_CASE));
        assertEquals(Capability.VERIFIED, snapshot.furnitureState(
                LivePohStateReader.PERMANENT_PORTAL));
        assertEquals(Capability.BLOCKED, snapshot.furnitureState(
                LivePohStateReader.PORTAL_NEXUS));
        assertEquals(Capability.VERIFIED, snapshot.furnitureState(
                LivePohStateReader.RESTORATION_POOL));
        assertEquals(Capability.VERIFIED, snapshot.furnitureState(
                LivePohStateReader.ORNATE_POOL));
        assertEquals(Capability.VERIFIED, snapshot.furnitureState(
                LivePohStateReader.ORNATE_JEWELLERY_BOX));
        assertEquals(Capability.VERIFIED, snapshot.furnitureState(
                LivePohStateReader.OCCULT_ALTAR));
        assertEquals(Capability.VERIFIED, snapshot.furnitureState(
                LivePohStateReader.FAIRY_RING));
        assertEquals(Capability.VERIFIED, snapshot.furnitureState(
                LivePohStateReader.SPIRIT_TREE));
        assertEquals(Capability.VERIFIED, snapshot.furnitureState(
                LivePohStateReader.SPIRITUAL_FAIRY_TREE));
    }

    @Test
    public void emptyPortalNexusDoesNotPretendAConfiguredRouteExists()
    {
        PohSnapshot snapshot = LivePohStateReader.snapshotForObjectIds(
                Collections.singleton(ObjectID.POH_NEXUS_PORTAL_1));

        assertEquals(Capability.VERIFIED, snapshot.furnitureState(
                LivePohStateReader.PORTAL_NEXUS));
        assertEquals(Capability.BLOCKED, snapshot.furnitureState(
                LivePohStateReader.PERMANENT_PORTAL));
    }
}
