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
        StrategyCandidate members = candidate("future:members",
                CandidateSafetyEvidence.harmless(false));
        assertTrue(evaluate(account(MembershipStatus.UNKNOWN, 70, 70, 70, 70,
                70, 70, 70, 70), members).isEmpty());
        assertTrue(evaluate(account(MembershipStatus.F2P, 70, 70, 70, 70,
                70, 70, 70, 70), members).isEmpty());
        assertEquals("future:members", evaluate(account(MembershipStatus.P2P,
                70, 70, 70, 70, 70, 70, 70, 70), members).get(0).getId());
    }

    @Test
    public void restrictedAndAmbiguousBuildsUseStructuredEffects()
    {
        AccountSnapshot oneDefence = account(MembershipStatus.P2P,
                60, 60, 1, 60, 43, 60, 70, 50);
        assertTrue(evaluate(oneDefence, candidate("future:unsafe",
                CandidateSafetyEvidence.potentiallyIrreversible(false))).isEmpty());
        assertFalse(evaluate(oneDefence, candidate("future:mining",
                CandidateSafetyEvidence.skill(false, Skill.MINING))).isEmpty());

        AccountSnapshot ambiguousRangeTank = account(MembershipStatus.P2P,
                50, 50, 75, 90, 70, 70, 85, 50);
        assertTrue(evaluate(ambiguousRangeTank, candidate("future:unsafe",
                CandidateSafetyEvidence.potentiallyIrreversible(false))).isEmpty());
        assertFalse(evaluate(ambiguousRangeTank, candidate("future:harmless",
                CandidateSafetyEvidence.harmless(false))).isEmpty());

        AccountSnapshot combatOnly = account(MembershipStatus.P2P,
                70, 70, 70, 70, 70, 70, 80, 1);
        assertTrue(evaluate(combatOnly, candidate("future:farming",
                CandidateSafetyEvidence.skill(false, Skill.FARMING))).isEmpty());

        for (AccountSnapshot restricted : java.util.Arrays.asList(
                account(MembershipStatus.P2P, 1, 1, 60, 1, 1, 1, 70, 50),
                account(MembershipStatus.P2P, 1, 1, 1, 1, 1, 1, 10, 50),
                account(MembershipStatus.P2P, 1, 1, 1, 1, 20, 1, 10, 50),
                account(MembershipStatus.P2P, 1, 1, 1, 20, 1, 20, 10, 50)))
        {
            assertTrue(evaluate(restricted, candidate("future:unsafe",
                    CandidateSafetyEvidence.potentiallyIrreversible(false))).isEmpty());
            assertFalse(evaluate(restricted, candidate("future:harmless",
                    CandidateSafetyEvidence.harmless(false))).isEmpty());
        }
    }

    @Test
    public void providerVerifiedSafeProgressionSurvivesFinalGate()
    {
        AccountSnapshot defencePure = account(MembershipStatus.P2P,
                1, 1, 60, 1, 43, 1, 70, 50);
        List<Recommendation> queue = evaluate(defencePure,
                candidate("upgrade:fighter-torso",
                        CandidateSafetyEvidence.verifiedSafe(false)));
        assertEquals("upgrade:fighter-torso", queue.get(0).getId());
    }

    @Test
    public void actualUpgradeProviderAllowsDefencePureFighterTorso()
    {
        AccountSnapshot defencePure = account(MembershipStatus.P2P,
                1, 1, 60, 1, 43, 1, 70, 50);
        StrategyDataBundle data = StrategyDataBundle.builder(defencePure)
                .bank(new BankSnapshot(Collections.emptyList(), 1L)).build();
        StrategyEngine engine = new StrategyEngine(null, null, null,
                new StrategyCandidateRegistry(Collections.singletonList(
                        new ProgressionUpgradeCandidateProvider())),
                new RecommendationActionabilityPolicy());
        List<Recommendation> queue = engine.evaluate(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, new PreferenceProfile())
                .getRecommendations();
        assertTrue(queue.stream().anyMatch(value ->
                "upgrade:fighter-torso".equals(value.getId())));
    }

    @Test
    public void blockedPvmNeverAppearsInFinalQueue()
    {
        StrategyCandidate blocked = new StrategyCandidate("pvm:obor", "Do Obor",
                "blocked", 1000, RecommendationConfidence.BLOCKED,
                guidance(), CandidateSafetyEvidence.potentiallyIrreversible(true));
        assertTrue(evaluate(account(MembershipStatus.P2P, 70, 70, 70, 70,
                70, 70, 70, 70), blocked).isEmpty());
    }

    private static List<Recommendation> evaluate(AccountSnapshot account,
            StrategyCandidate candidate)
    {
        StrategyCandidateProvider provider = new StrategyCandidateProvider()
        {
            @Override public String getId() { return "test-provider"; }
            @Override public List<StrategyCandidate> candidates(StrategyContext context)
            { return Collections.singletonList(candidate); }
        };
        StrategyEngine engine = new StrategyEngine(null, null, null,
                new StrategyCandidateRegistry(Collections.singletonList(provider)),
                new RecommendationActionabilityPolicy());
        return engine.evaluate(StrategyDataBundle.builder(account).build(),
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                new PreferenceProfile()).getRecommendations();
    }

    private static StrategyCandidate candidate(String id,
            CandidateSafetyEvidence evidence)
    {
        return new StrategyCandidate(id, id, "test", 50,
                RecommendationConfidence.VERIFIED, guidance(), evidence);
    }

    private static RecommendationGuidance guidance()
    {
        return new RecommendationGuidance("Do the verified step.", "Ready.",
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
