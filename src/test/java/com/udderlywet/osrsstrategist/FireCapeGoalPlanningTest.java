package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FireCapeGoalPlanningTest
{
    private final PvmCandidateProvider provider = new PvmCandidateProvider();

    @Test
    public void selectedGoalPromotesOnlyReadinessBackedFightCavePlan()
    {
        StrategyDataBundle data = data(0, MembershipStatus.P2P,
                new PvmReadiness("pvm:tztok_jad", true,
                        RecommendationConfidence.VERIFIED,
                        Collections.emptyList()));

        Recommendation automatic = find(provider.candidates(
                context(data, GoalType.AUTOMATIC)), "pvm:tztok_jad");
        Recommendation goal = find(provider.candidates(
                context(data, GoalType.FIRE_CAPE)), "pvm:tztok_jad");

        assertNotNull(automatic);
        assertNotNull(goal);
        assertTrue(goal.getScore() > automatic.getScore());
        assertTrue(goal.getGuidance().getAction().contains("63 waves"));
        assertTrue(goal.getGuidance().getLocation().contains("Fight Cave"));
        assertTrue(new RecommendationActionabilityPolicy()
                .canLeadQueue(goal));
    }

    @Test
    public void statsWithoutObservedReadinessCannotBecomeAFightCaveAttempt()
    {
        StrategyDataBundle data = data(0, MembershipStatus.P2P,
                new PvmReadiness("pvm:tztok_jad", false,
                        RecommendationConfidence.CHECK_NEEDED,
                        Collections.singletonList(
                                "Observe a carried Ranged weapon and wave supplies")));

        Recommendation candidate = find(provider.candidates(
                context(data, GoalType.FIRE_CAPE)), "pvm:tztok_jad");

        assertNotNull(candidate);
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                candidate.getConfidence());
        assertFalse(new RecommendationActionabilityPolicy()
                .canLeadQueue(candidate));
    }

    @Test
    public void f2pAndHardcoreAccountsDoNotReceiveFightCaveDoNext()
    {
        PvmReadiness ready = new PvmReadiness("pvm:tztok_jad", true,
                RecommendationConfidence.VERIFIED, Collections.emptyList());
        assertTrue(provider.candidates(context(
                data(0, MembershipStatus.F2P, ready), GoalType.FIRE_CAPE))
                .isEmpty());
        assertTrue(provider.candidates(context(
                data(3, MembershipStatus.P2P, ready), GoalType.FIRE_CAPE))
                .isEmpty());
    }

    private static StrategyContext context(
            StrategyDataBundle data, GoalType goal)
    {
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.LONG_SESSION, QuestTolerance.NORMAL, goal,
                false, false, new PreferenceProfile());
    }

    private static StrategyDataBundle data(int type,
            MembershipStatus membership, PvmReadiness readiness)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, skill == Skill.HITPOINTS ? 75 : 70);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Fire cape", 900L,
                type, AccountMode.fromTypeCode(type).name(), membership,
                membership == MembershipStatus.P2P ? 1 : 0,
                70 * Skill.values().length, 0L, levels, xp);
        Map<String, PvmReadiness> values = new HashMap<>();
        values.put("pvm:tztok_jad", readiness);
        return StrategyDataBundle.builder(account)
                .pvm(new PvmSnapshot(values)).build();
    }

    private static Recommendation find(
            List<Recommendation> candidates, String id)
    {
        for (Recommendation candidate : candidates)
            if (id.equals(candidate.getId())) return candidate;
        return null;
    }
}
