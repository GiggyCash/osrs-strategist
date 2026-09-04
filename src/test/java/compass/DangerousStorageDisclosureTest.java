package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DangerousStorageDisclosureTest
{
    private final UimCapabilityService capabilityService =
            new UimCapabilityService();
    private final CandidateSafetyPolicy safety = new CandidateSafetyPolicy();

    @Test
    public void exactVerifiedServiceStillNeedsProminentDisclosure()
    {
        StrategyContext context = context();
        UimStorageDecision decision = capabilityService.evaluateStorage(
                context.data(), StorageKind.HESPORI_ITEM_RETRIEVAL,
                Capability.VERIFIED, Capability.VERIFIED);
        Recommendation withoutWarning = recommendation(
                guidance(decision, null));

        assertFalse(safety.isAllowed(withoutWarning, context));

        RecommendationRiskDisclosure disclosure =
                RecommendationRiskDisclosure.deathStorage();
        Recommendation warned = recommendation(guidance(decision, disclosure));
        assertTrue(safety.isAllowed(warned, context));
        assertTrue(Presentation.compactText(warned)
                .contains("HIGH RISK"));
        assertTrue(Presentation.compactText(warned)
                .contains("acknowledge"));
    }

    @Test
    public void genericDeathStorageFailsEvenWithWarning()
    {
        StrategyContext context = context();
        UimStorageDecision generic = new UimStorageDecision(
                StorageKind.DEATH_STORAGE, true,
                Confidence.VERIFIED, RiskLevel.HIGH,
                "Synthetic generic evidence");
        Recommendation result = recommendation(guidance(generic,
                RecommendationRiskDisclosure.deathStorage()));
        assertFalse(safety.isAllowed(result, context));
    }

    private static Guidance guidance(UimStorageDecision decision,
            RecommendationRiskDisclosure disclosure)
    {
        return new Guidance("View the verified steps.",
                "Exact observed setup", "Hespori cave",
                "Do not begin until every retrieval rule is understood.",
                BankingMode.UNKNOWN, decision, disclosure);
    }

    private static Recommendation recommendation(
            Guidance guidance)
    {
        return new Recommendation("uim:dangerous-storage", "Storage transition",
                "A major progression transition is otherwise blocked.", 10.0,
                null, Confidence.VERIFIED, 0, 0, guidance,
                Safety.harmless(false));
    }

    private static StrategyContext context()
    {
        Map<StorageKind, Capability> states =
                new EnumMap<>(StorageKind.class);
        states.put(StorageKind.HESPORI_ITEM_RETRIEVAL,
                Capability.VERIFIED);
        GameData data = GameData.builder(uim())
                .inventory(new ItemsState(Collections.emptyList()))
                .storage(new StorageSnapshot(states, Collections.emptyMap()))
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false,
                new PreferenceProfile());
    }

    private static AccountSnapshot uim()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 50);
            xp.put(skill, 101_333);
        }
        return new AccountSnapshot("Uim", 0L, 2, "Ultimate Ironman", Membership.P2P, 50, 50, 101_333L, levels, xp);
    }
}
