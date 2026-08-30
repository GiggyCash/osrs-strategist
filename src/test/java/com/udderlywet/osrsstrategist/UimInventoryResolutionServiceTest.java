package com.udderlywet.osrsstrategist;

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
    private final MethodInventoryFootprint needsFour =
            new MethodInventoryFootprint(4, 2, 3,
                    InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, true);

    @Test
    public void asIsAndComparableMethodComeBeforeStorage()
    {
        assertEquals(UimInventoryResolutionKind.USE_AS_IS,
                service.resolve(data(20), needsFour, false, false,
                        Collections.emptyList()).getKind());
        assertEquals(UimInventoryResolutionKind.USE_LOW_FOOTPRINT_ALTERNATIVE,
                service.resolve(data(28), needsFour, true, true,
                        Arrays.asList(option(StorageCapability.DEATHPILE,
                                true, StrategicPriority.CRITICAL)))
                        .getKind());
    }

    @Test
    public void productiveUseAndVerifiedSafeStorageOutrankDanger()
    {
        assertEquals(UimInventoryResolutionKind.PRODUCTIVELY_CONSUME_RESOURCES,
                service.resolve(data(28), needsFour, false, true,
                        Collections.emptyList()).getKind());

        StrategyDataBundle data = data(28,
                StorageCapability.POH_COSTUME_ROOM,
                StorageCapability.HESPORI_ITEM_RETRIEVAL);
        UimInventoryResolution result = service.resolve(data, needsFour,
                false, false, Arrays.asList(
                        option(StorageCapability.HESPORI_ITEM_RETRIEVAL,
                                true, StrategicPriority.NONE),
                        option(StorageCapability.POH_COSTUME_ROOM,
                                false, StrategicPriority.HIGH)));
        assertEquals(UimInventoryResolutionKind.USE_VERIFIED_SAFE_STORAGE,
                result.getKind());
        assertEquals(StorageCapability.POH_COSTUME_ROOM,
                result.getStorageDecision().getCapability());
    }

    @Test
    public void worthwhileSafeInfrastructureAndLootingBagPrecedeDeathStorage()
    {
        StrategyDataBundle data = data(28, StorageCapability.LOOTING_BAG,
                StorageCapability.HESPORI_ITEM_RETRIEVAL);
        UimInventoryResolution build = service.resolve(data, needsFour,
                false, false, Arrays.asList(
                        option(StorageCapability.POH_STORAGE, true,
                                StrategicPriority.HIGH),
                        option(StorageCapability.HESPORI_ITEM_RETRIEVAL,
                                true, StrategicPriority.NONE)));
        assertEquals(UimInventoryResolutionKind.BUILD_HIGH_VALUE_SAFE_STORAGE,
                build.getKind());

        UimInventoryResolution bag = service.resolve(data, needsFour,
                false, false, Arrays.asList(
                        option(StorageCapability.LOOTING_BAG, false,
                                StrategicPriority.NONE),
                        option(StorageCapability.HESPORI_ITEM_RETRIEVAL,
                                true, StrategicPriority.NONE)));
        assertEquals(UimInventoryResolutionKind.USE_RESTRICTED_RETRIEVAL,
                bag.getKind());
    }

    @Test
    public void dangerousStorageRequiresExactVerifiedMajorTransition()
    {
        StrategyDataBundle data = data(28,
                StorageCapability.HESPORI_ITEM_RETRIEVAL,
                StorageCapability.DEATH_STORAGE);
        UimInventoryResolution ordinary = service.resolve(data, needsFour,
                false, false, Collections.singletonList(
                        option(StorageCapability.HESPORI_ITEM_RETRIEVAL,
                                false, StrategicPriority.NONE)));
        assertEquals(UimInventoryResolutionKind.UNRESOLVED,
                ordinary.getKind());
        assertTrue(ordinary.getReason().contains("will not recommend banking"));

        UimInventoryResolution major = service.resolve(data, needsFour,
                false, false, Collections.singletonList(
                        option(StorageCapability.HESPORI_ITEM_RETRIEVAL,
                                true, StrategicPriority.NONE)));
        assertEquals(UimInventoryResolutionKind.USE_DANGEROUS_DEATH_STORAGE,
                major.getKind());
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                major.getConfidence());
        assertNotNull(major.getRiskDisclosure());
        assertTrue(major.getRiskDisclosure().isAcknowledgementRequired());

        UimInventoryResolution generic = service.resolve(data, needsFour,
                false, false, Collections.singletonList(
                        option(StorageCapability.DEATH_STORAGE,
                                true, StrategicPriority.NONE)));
        assertEquals(UimInventoryResolutionKind.UNRESOLVED,
                generic.getKind());
    }

    private static UimStorageOption option(StorageCapability capability,
            boolean major, StrategicPriority value)
    {
        boolean build = capability == StorageCapability.POH_STORAGE;
        return new UimStorageOption(capability, CapabilityState.VERIFIED,
                CapabilityState.VERIFIED, build, value, major);
    }

    private static StrategyDataBundle data(int occupied,
            StorageCapability... verified)
    {
        List<ItemStackSnapshot> items = new ArrayList<>();
        for (int slot = 0; slot < occupied; slot++)
            items.add(new ItemStackSnapshot(30_000 + slot,
                    "Persistent item " + slot, 1, slot));
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        for (StorageCapability capability : verified)
            states.put(capability, CapabilityState.VERIFIED);
        return StrategyDataBundle.builder(uim())
                .inventory(new InventorySnapshot(items, true))
                .storage(new StorageSnapshot(states))
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
        return new AccountSnapshot("Uim", 2, "Ultimate Ironman",
                MembershipStatus.P2P, 1, 70 * Skill.values().length,
                0L, levels, xp);
    }
}
