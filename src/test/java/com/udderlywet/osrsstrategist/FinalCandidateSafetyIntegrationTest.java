package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FinalCandidateSafetyIntegrationTest
{
    @Test
    public void futureProviderCannotLeakMembersContentToUnknownOrF2p()
    {
        Recommendation members = candidate("future:members",
                SafetyEvidence.harmless(false));
        assertOnlyFallback(evaluate(account(MembershipStatus.UNKNOWN, 70, 70, 70, 70,
                70, 70, 70, 70), members));
        assertOnlyFallback(evaluate(account(MembershipStatus.F2P, 70, 70, 70, 70,
                70, 70, 70, 70), members));
        assertEquals("future:members", evaluate(account(MembershipStatus.P2P,
                70, 70, 70, 70, 70, 70, 70, 70), members).get(0).getId());
    }

    @Test
    public void restrictedAndAmbiguousBuildsUseStructuredEffects()
    {
        AccountSnapshot oneDefence = account(MembershipStatus.P2P,
                60, 60, 1, 60, 43, 60, 70, 50);
        assertOnlyFallback(evaluate(oneDefence, candidate("future:unsafe",
                SafetyEvidence.potentiallyIrreversible(false))));
        assertFalse(evaluate(oneDefence, candidate("future:mining",
                SafetyEvidence.skill(false, Skill.MINING))).isEmpty());

        AccountSnapshot ambiguousRangeTank = account(MembershipStatus.P2P,
                50, 50, 75, 90, 70, 70, 85, 50);
        assertOnlyFallback(evaluate(ambiguousRangeTank, candidate("future:unsafe",
                SafetyEvidence.potentiallyIrreversible(false))));
        assertFalse(evaluate(ambiguousRangeTank, candidate("future:harmless",
                SafetyEvidence.harmless(false))).isEmpty());

        AccountSnapshot combatOnly = account(MembershipStatus.P2P,
                70, 70, 70, 70, 70, 70, 80, 1);
        assertOnlyFallback(evaluate(combatOnly, candidate("future:farming",
                SafetyEvidence.skill(false, Skill.FARMING))));

        for (AccountSnapshot restricted : java.util.Arrays.asList(
                account(MembershipStatus.P2P, 1, 1, 60, 1, 1, 1, 70, 50),
                account(MembershipStatus.P2P, 1, 1, 1, 1, 1, 1, 10, 50),
                account(MembershipStatus.P2P, 1, 1, 1, 1, 20, 1, 10, 50),
                account(MembershipStatus.P2P, 1, 1, 1, 20, 1, 20, 10, 50)))
        {
            assertOnlyFallback(evaluate(restricted, candidate("future:unsafe",
                    SafetyEvidence.potentiallyIrreversible(false))));
            assertFalse(evaluate(restricted, candidate("future:harmless",
                    SafetyEvidence.harmless(false))).isEmpty());
        }
    }

    @Test
    public void providerVerifiedSafeProgressionSurvivesFinalGate()
    {
        AccountSnapshot defencePure = account(MembershipStatus.P2P,
                1, 1, 60, 1, 43, 1, 70, 50);
        List<Recommendation> queue = evaluate(defencePure,
                candidate("upgrade:fighter-torso",
                        SafetyEvidence.verifiedSafe(false)));
        assertEquals("upgrade:fighter-torso", queue.get(0).getId());
    }

    @Test
    public void actualUpgradeProviderAllowsDefencePureFighterTorso()
    {
        AccountSnapshot defencePure = account(MembershipStatus.P2P,
                1, 1, 60, 1, 43, 1, 70, 50);
        GameData data = GameData.builder(defencePure)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build();
        StrategyEngine engine = new StrategyEngine(null, null, null,
                new StrategyCandidateRegistry(Collections.singletonList(
                        new ProgressionUpgradeCandidateProvider())),
                new ActionabilityPolicy());
        List<Recommendation> queue = engine.evaluate(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, new PreferenceProfile())
                .getRecommendations();
        assertTrue(queue.stream().anyMatch(value ->
                "upgrade:fighter-torso".equals(value.getId())));
    }

    @Test
    public void blockedPvmNeverAppearsInFinalQueue()
    {
        Recommendation blocked = new Recommendation("pvm:obor", "Do Obor",
                "blocked", 1000, Confidence.BLOCKED,
                guidance(), SafetyEvidence.potentiallyIrreversible(true));
        assertOnlyFallback(evaluate(account(MembershipStatus.P2P, 70, 70, 70, 70,
                70, 70, 70, 70), blocked));
    }

    private static List<Recommendation> evaluate(AccountSnapshot account,
            Recommendation candidate)
    {
        CandidateProvider provider = new CandidateProvider()
        {
            @Override public String getId() { return "test-provider"; }
            @Override public List<Recommendation> candidates(StrategyContext context)
            { return Collections.singletonList(candidate); }
        };
        StrategyEngine engine = new StrategyEngine(null, null, null,
                new StrategyCandidateRegistry(Collections.singletonList(provider)),
                new ActionabilityPolicy());
        return engine.evaluate(GameData.builder(account).build(),
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                new PreferenceProfile()).getRecommendations();
    }

    private static void assertOnlyFallback(List<Recommendation> queue)
    {
        assertEquals(1, queue.size());
        assertTrue(FallbackRecommendationFactory.isFallback(queue.get(0)));
    }

    private static Recommendation candidate(String id,
            SafetyEvidence evidence)
    {
        return new Recommendation(id, id, "test", 50,
                Confidence.VERIFIED, guidance(), evidence);
    }

    private static Guidance guidance()
    {
        return new Guidance("Do the verified step.", "Ready.",
                "Safe location.", "Test evidence.");
    }

    private static AccountSnapshot account(MembershipStatus membership,
            int attack, int strength, int defence, int ranged, int prayer,
            int magic, int hitpoints, int nonCombat)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, nonCombat); xp.put(skill, 0); }
        levels.put(Skill.ATTACK, attack); levels.put(Skill.STRENGTH, strength);
        levels.put(Skill.DEFENCE, defence); levels.put(Skill.RANGED, ranged);
        levels.put(Skill.PRAYER, prayer); levels.put(Skill.MAGIC, magic);
        levels.put(Skill.HITPOINTS, hitpoints); levels.put(Skill.SLAYER, 1);
        return new AccountSnapshot("Safety", 0, "Main", membership,
                1, 1000, 0L, levels, xp);
    }
}
