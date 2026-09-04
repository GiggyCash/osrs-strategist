package compass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UimInventoryResolutionServiceTest
{
    private final UimInventoryResolutionService service =
            new UimInventoryResolutionService();
    private final InventoryFootprint needsFour =
            new InventoryFootprint(4, 2, 3,
                    InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, true);

    @Test
    public void asIsAndComparableMethodComeBeforeStorage()
    {
        assertEquals(UimInventoryKind.USE_AS_IS,
                service.resolve(data(20), needsFour, false, false,
                        Collections.emptyList()).getKind());
        assertEquals(UimInventoryKind.USE_LOW_FOOTPRINT_ALTERNATIVE,
                service.resolve(data(28), needsFour, true, true,
                        Arrays.asList(option(StorageKind.DEATHPILE,
                                true, Priority.CRITICAL)))
                        .getKind());
    }

    @Test
    public void productiveUseAndVerifiedSafeStorageOutrankDanger()
    {
        assertEquals(UimInventoryKind.PRODUCTIVELY_CONSUME_RESOURCES,
                service.resolve(data(28), needsFour, false, true,
                        Collections.emptyList()).getKind());

        GameData data = data(28,
                StorageKind.POH_COSTUME_ROOM,
                StorageKind.HESPORI_ITEM_RETRIEVAL);
        UimInventoryResolution result = service.resolve(data, needsFour,
                false, false, Arrays.asList(
                        option(StorageKind.HESPORI_ITEM_RETRIEVAL,
                                true, Priority.NONE),
                        option(StorageKind.POH_COSTUME_ROOM,
                                false, Priority.HIGH)));
        assertEquals(UimInventoryKind.USE_VERIFIED_SAFE_STORAGE,
                result.getKind());
        assertEquals(StorageKind.POH_COSTUME_ROOM,
                result.getStorageDecision().getCapability());
    }

    @Test
    public void worthwhileSafeInfrastructureAndLootingBagPrecedeDeathStorage()
    {
        GameData data = data(28, StorageKind.LOOTING_BAG,
                StorageKind.HESPORI_ITEM_RETRIEVAL);
        UimInventoryResolution build = service.resolve(data, needsFour,
                false, false, Arrays.asList(
                        option(StorageKind.POH_STORAGE, true,
                                Priority.HIGH),
                        option(StorageKind.HESPORI_ITEM_RETRIEVAL,
                                true, Priority.NONE)));
        assertEquals(UimInventoryKind.BUILD_HIGH_VALUE_SAFE_STORAGE,
                build.getKind());

        UimInventoryResolution bag = service.resolve(data, needsFour,
                false, false, Arrays.asList(
                        option(StorageKind.LOOTING_BAG, false,
                                Priority.NONE),
                        option(StorageKind.HESPORI_ITEM_RETRIEVAL,
                                true, Priority.NONE)));
        assertEquals(UimInventoryKind.USE_RESTRICTED_RETRIEVAL,
                bag.getKind());
    }

    @Test
    public void dangerousStorageRequiresExactVerifiedMajorTransition()
    {
        GameData data = data(28,
                StorageKind.HESPORI_ITEM_RETRIEVAL,
                StorageKind.DEATH_STORAGE);
        UimInventoryResolution ordinary = service.resolve(data, needsFour,
                false, false, Collections.singletonList(
                        option(StorageKind.HESPORI_ITEM_RETRIEVAL,
                                false, Priority.NONE)));
        assertEquals(UimInventoryKind.UNRESOLVED,
                ordinary.getKind());
        assertTrue(ordinary.getReason().contains("will not recommend banking"));

        UimInventoryResolution major = service.resolve(data, needsFour,
                false, false, Collections.singletonList(
                        option(StorageKind.HESPORI_ITEM_RETRIEVAL,
                                true, Priority.NONE)));
        assertEquals(UimInventoryKind.USE_DANGEROUS_DEATH_STORAGE,
                major.getKind());
        assertEquals(Confidence.CHECK_NEEDED,
                major.getConfidence());
        assertNotNull(major.getRiskDisclosure());
        assertTrue(major.getRiskDisclosure().isAcknowledgementRequired());

        UimInventoryResolution generic = service.resolve(data, needsFour,
                false, false, Collections.singletonList(
                        option(StorageKind.DEATH_STORAGE,
                                true, Priority.NONE)));
        assertEquals(UimInventoryKind.UNRESOLVED,
                generic.getKind());
    }

    private static UimStorageOption option(StorageKind capability,
            boolean major, Priority value)
    {
        boolean build = capability == StorageKind.POH_STORAGE;
        return new UimStorageOption(capability, Capability.VERIFIED,
                Capability.VERIFIED, build, value, major);
    }

    private static GameData data(int occupied,
            StorageKind... verified)
    {
        List<ItemState> items = new ArrayList<>();
        for (int slot = 0; slot < occupied; slot++)
            items.add(new ItemState(30_000 + slot,
                    "Persistent item " + slot, 1, slot));
        Map<StorageKind, Capability> states =
                new EnumMap<>(StorageKind.class);
        for (StorageKind capability : verified)
            states.put(capability, Capability.VERIFIED);
        return GameData.builder(uim())
                .inventory(new ItemsState(items, true))
                .storage(new StorageSnapshot(states, Collections.emptyMap()))
                .build();
    }

    private static AccountSnapshot uim()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        return new AccountSnapshot("Uim", 0L, 2, "Ultimate Ironman", Membership.P2P, 1, 70 * Skill.values().length, 0L, levels, xp);
    }
}
