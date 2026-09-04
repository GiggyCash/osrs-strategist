package compass;

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
        GameData data = GameData.builder(account(2))
                .storage(StorageSnapshot.unknown())
                .build();

        UimStorageDecision decision = service.evaluateStorage(
                data,
                StorageKind.STASH,
                Capability.VERIFIED,
                Capability.VERIFIED
        );

        assertFalse(decision.isAllowed());
        assertTrue(decision.getConfidence()
                == Confidence.CHECK_NEEDED);
    }

    @Test
    public void requiresCapabilityCompatibilityAndCapacityTogether()
    {
        Map<StorageKind, Capability> states =
                new EnumMap<>(StorageKind.class);
        states.put(StorageKind.POH_STORAGE, Capability.VERIFIED);
        GameData data = GameData.builder(account(2))
                .storage(new StorageSnapshot(states, Collections.emptyMap()))
                .build();

        assertFalse(service.evaluateStorage(
                data,
                StorageKind.POH_STORAGE,
                Capability.UNKNOWN,
                Capability.VERIFIED
        ).isAllowed());

        assertTrue(service.evaluateStorage(
                data,
                StorageKind.POH_STORAGE,
                Capability.VERIFIED,
                Capability.VERIFIED
        ).isAllowed());
    }

    @Test
    public void deathpileAlwaysCarriesIrreversibleRisk()
    {
        assertTrue(service.shouldRequireExplicitWarning(
                StorageKind.DEATHPILE));
    }

    @Test
    public void genericDeathStorageCanNeverAuthorizeAPlan()
    {
        Map<StorageKind, Capability> states =
                new EnumMap<>(StorageKind.class);
        states.put(StorageKind.DEATH_STORAGE,
                Capability.VERIFIED);
        GameData data = GameData.builder(account(2))
                .storage(new StorageSnapshot(states, Collections.emptyMap()))
                .build();

        UimStorageDecision decision = service.evaluateStorage(data,
                StorageKind.DEATH_STORAGE,
                Capability.VERIFIED, Capability.VERIFIED);
        assertFalse(decision.isAllowed());
        assertTrue(decision.getExplanation().contains("exact service"));
    }

    @Test
    public void exactRetrievalServiceStillRequiresAllEvidence()
    {
        Map<StorageKind, Capability> states =
                new EnumMap<>(StorageKind.class);
        states.put(StorageKind.HESPORI_ITEM_RETRIEVAL,
                Capability.VERIFIED);
        GameData data = GameData.builder(account(2))
                .storage(new StorageSnapshot(states, Collections.emptyMap()))
                .build();

        assertFalse(service.evaluateStorage(data,
                StorageKind.HESPORI_ITEM_RETRIEVAL,
                Capability.UNKNOWN, Capability.VERIFIED)
                .isAllowed());
        assertTrue(service.evaluateStorage(data,
                StorageKind.HESPORI_ITEM_RETRIEVAL,
                Capability.VERIFIED, Capability.VERIFIED)
                .isAllowed());
        assertTrue(service.shouldRequireExplicitWarning(
                StorageKind.HESPORI_ITEM_RETRIEVAL));
    }

    @Test
    public void everyDangerousSystemHasCompleteDistinctReviewedMechanics()
    {
        StorageKind[] exact = {
                StorageKind.HESPORI_ITEM_RETRIEVAL,
                StorageKind.ZULRAH_ITEM_RETRIEVAL,
                StorageKind.VOLCANIC_MINE_ITEM_RETRIEVAL,
                StorageKind.DEATHPILE
        };
        for (StorageKind capability : exact)
        {
            UimStorageMechanicProfile profile =
                    UimStorageMechanics.profile(capability);
            assertTrue(capability.name(), profile != null);
            assertTrue(capability.name(),
                    profile.hasCompleteRecommendationRules());
            assertTrue(capability.name(),
                    profile.getSource() == Source.ITEM_RETRIEVAL_SERVICES
                    || profile.getSource()
                            == Source.UIM_ITEM_MANAGEMENT);
        }
        assertTrue(UimStorageMechanics.profile(
                StorageKind.ZULRAH_ITEM_RETRIEVAL).getCost()
                .contains("Free for Ultimate Ironmen"));
        assertTrue(UimStorageMechanics.profile(
                StorageKind.VOLCANIC_MINE_ITEM_RETRIEVAL).getCost()
                .contains("150 numulite"));
        assertTrue(UimStorageMechanics.profile(
                StorageKind.HESPORI_ITEM_RETRIEVAL).getCost()
                .contains("25,000 coins"));
        assertTrue(UimStorageMechanics.profile(StorageKind.DEATHPILE)
                .getExpiration().contains("60 minutes"));
        assertFalse(UimStorageMechanics.profile(
                StorageKind.DEATH_STORAGE)
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
        return new AccountSnapshot("Tester", 0L, typeCode, "UIM", Membership.P2P, 1, 1, 0L, levels, xp);
    }
}
