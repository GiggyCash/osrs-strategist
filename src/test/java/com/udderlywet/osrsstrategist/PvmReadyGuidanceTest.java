package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmReadyGuidanceTest
{
    @Test
    public void locallyVerifiedEncountersNameTheirActualEntrance()
    {
        for (String id : new String[]{"pvm:obor", "pvm:bryophyta",
                "pvm:scurrius"})
        {
            List<Recommendation> candidates = new PvmCandidateProvider()
                    .candidates(context(id));
            assertFalse(id, candidates.isEmpty());
            Recommendation recommendation = candidates.get(0);
            assertTrue(id + " lacks presentable guidance",
                    new RecommendationQualityPolicy().isPresentable(
                            recommendation));
            assertFalse(recommendation.getGuidance().getLocation()
                    .contains("exact non-Wilderness route recorded"));
        }
    }

    private static StrategyContext context(String readyId)
    {
        Map<String, PvmReadiness> readiness = new HashMap<>();
        readiness.put(readyId, new PvmReadiness(readyId, true,
                RecommendationConfidence.VERIFIED,
                Collections.emptyList()));
        StrategyDataBundle data = StrategyDataBundle.builder(account())
                .pvm(new PvmSnapshot(readiness)).build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false, false,
                new PreferenceProfile());
    }

    private static AccountSnapshot account()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        return new AccountSnapshot("PvM", 0, "Main",
                MembershipStatus.P2P, 1, 1600, 0L, levels, xp);
    }
}
