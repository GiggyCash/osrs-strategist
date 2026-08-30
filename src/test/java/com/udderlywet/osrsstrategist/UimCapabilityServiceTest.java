package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UimCapabilityServiceTest
{
    private final UimCapabilityService service = new UimCapabilityService();

    @Test
    public void refusesUnknownUimStorageCapability()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account(2))
                .storage(StorageSnapshot.unknown())
                .build();

        UimStorageDecision decision = service.evaluateStorage(
                data,
                StorageCapability.STASH,
                CapabilityState.VERIFIED,
                CapabilityState.VERIFIED
        );

        assertFalse(decision.isAllowed());
        assertTrue(decision.getConfidence()
                == RecommendationConfidence.CHECK_NEEDED);
    }

    @Test
    public void requiresCapabilityCompatibilityAndCapacityTogether()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.POH_STORAGE, CapabilityState.VERIFIED);
        StrategyDataBundle data = StrategyDataBundle.builder(account(2))
                .storage(new StorageSnapshot(states))
                .build();

        assertFalse(service.evaluateStorage(
                data,
                StorageCapability.POH_STORAGE,
                CapabilityState.UNKNOWN,
                CapabilityState.VERIFIED
        ).isAllowed());

        assertTrue(service.evaluateStorage(
                data,
                StorageCapability.POH_STORAGE,
                CapabilityState.VERIFIED,
                CapabilityState.VERIFIED
        ).isAllowed());
    }

    @Test
    public void deathpileAlwaysCarriesIrreversibleRisk()
    {
        assertTrue(service.shouldRequireExplicitWarning(
                StorageCapability.DEATHPILE));
    }

    @Test
    public void genericDeathStorageCanNeverAuthorizeAPlan()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.DEATH_STORAGE,
                CapabilityState.VERIFIED);
        StrategyDataBundle data = StrategyDataBundle.builder(account(2))
                .storage(new StorageSnapshot(states))
                .build();

        UimStorageDecision decision = service.evaluateStorage(data,
                StorageCapability.DEATH_STORAGE,
                CapabilityState.VERIFIED, CapabilityState.VERIFIED);
        assertFalse(decision.isAllowed());
        assertTrue(decision.getExplanation().contains("exact service"));
    }

    @Test
    public void exactRetrievalServiceStillRequiresAllEvidence()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.HESPORI_ITEM_RETRIEVAL,
                CapabilityState.VERIFIED);
        StrategyDataBundle data = StrategyDataBundle.builder(account(2))
                .storage(new StorageSnapshot(states))
                .build();

        assertFalse(service.evaluateStorage(data,
                StorageCapability.HESPORI_ITEM_RETRIEVAL,
                CapabilityState.UNKNOWN, CapabilityState.VERIFIED)
                .isAllowed());
        assertTrue(service.evaluateStorage(data,
                StorageCapability.HESPORI_ITEM_RETRIEVAL,
                CapabilityState.VERIFIED, CapabilityState.VERIFIED)
                .isAllowed());
        assertTrue(service.shouldRequireExplicitWarning(
                StorageCapability.HESPORI_ITEM_RETRIEVAL));
    }

    @Test
    public void everyDangerousSystemHasCompleteDistinctReviewedMechanics()
    {
        StorageCapability[] exact = {
                StorageCapability.HESPORI_ITEM_RETRIEVAL,
                StorageCapability.ZULRAH_ITEM_RETRIEVAL,
                StorageCapability.VOLCANIC_MINE_ITEM_RETRIEVAL,
                StorageCapability.DEATHPILE
        };
        for (StorageCapability capability : exact)
        {
            UimStorageMechanicProfile profile =
                    UimStorageMechanics.profile(capability);
            assertTrue(capability.name(), profile != null);
            assertTrue(capability.name(),
                    profile.hasCompleteRecommendationRules());
            assertTrue(capability.name(),
                    profile.getSource() == StrategySourceId.ITEM_RETRIEVAL_SERVICES
                    || profile.getSource()
                            == StrategySourceId.UIM_ITEM_MANAGEMENT);
        }
        assertTrue(UimStorageMechanics.profile(
                StorageCapability.ZULRAH_ITEM_RETRIEVAL).getCost()
                .contains("Free for Ultimate Ironmen"));
        assertTrue(UimStorageMechanics.profile(
                StorageCapability.VOLCANIC_MINE_ITEM_RETRIEVAL).getCost()
                .contains("150 numulite"));
        assertTrue(UimStorageMechanics.profile(
                StorageCapability.HESPORI_ITEM_RETRIEVAL).getCost()
                .contains("25,000 coins"));
        assertTrue(UimStorageMechanics.profile(StorageCapability.DEATHPILE)
                .getExpiration().contains("60 minutes"));
        assertFalse(UimStorageMechanics.profile(
                StorageCapability.DEATH_STORAGE)
                .hasCompleteRecommendationRules());
    }

    private static AccountSnapshot account(int typeCode)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(
                "Tester", typeCode, "UIM", MembershipStatus.P2P,
                1, 1, 0L, levels, xp
        );
    }
}
