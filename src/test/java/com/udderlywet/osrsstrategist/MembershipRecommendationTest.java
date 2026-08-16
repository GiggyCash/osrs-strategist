package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class MembershipRecommendationTest
{
    private final TrainingMethodSelector selector =
            new TrainingMethodSelector(new TrainingMethodDatabase());

    private final RecommendationEngine engine =
            new RecommendationEngine(selector);

    @Test
    public void f2pRecommendationQueueContainsOnlyFreeSkills()
    {
        AccountSnapshot account = account(
                MembershipStatus.F2P,
                30
        );

        List<Recommendation> recommendations = engine.recommend(
                account,
                StrategyMode.RELAXED,
                SessionIntent.AFK,
                new PreferenceProfile()
        );

        assertFalse(recommendations.isEmpty());
        assertEquals("skill:mining", recommendations.get(0).getId());

        for (Recommendation recommendation : recommendations)
        {
            Skill skill = MilestoneTracker.skillFor(recommendation);
            assertNotNull(skill);
            assertFalse(skill == Skill.FARMING);
            assertFalse(skill == Skill.HERBLORE);
            assertFalse(skill == Skill.SAILING);
        }
    }

    @Test
    public void f2pMiningDoesNotChooseMotherlodeMine()
    {
        AccountSnapshot account = account(
                MembershipStatus.F2P,
                30
        );

        TrainingPlan plan = selector.select(
                StrategyDataBundle.builder(account).build(),
                Skill.MINING,
                30,
                StrategyMode.RELAXED,
                SessionIntent.AFK
        );

        assertNotNull(plan);
        assertEquals("mining_ore", plan.getMethod().getId());
    }

    @Test
    public void p2pMiningCanChooseMotherlodeMine()
    {
        AccountSnapshot account = account(
                MembershipStatus.P2P,
                30
        );

        TrainingPlan plan = selector.select(
                StrategyDataBundle.builder(account).build(),
                Skill.MINING,
                30,
                StrategyMode.RELAXED,
                SessionIntent.AFK
        );

        assertNotNull(plan);
        assertEquals("mining_mlm", plan.getMethod().getId());
    }

    private static AccountSnapshot account(
            MembershipStatus membershipStatus,
            int miningLevel)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> experience = new EnumMap<>(Skill.class);

        for (Skill skill : Skill.values())
        {
            levels.put(skill, 99);
            experience.put(skill, 0);
        }

        // Only Mining should remain as a trainable F2P skill. Members skills are
        // deliberately low to prove that the membership gate removes them.
        levels.put(Skill.MINING, miningLevel);
        levels.put(Skill.AGILITY, 1);
        levels.put(Skill.HERBLORE, 1);
        levels.put(Skill.THIEVING, 1);
        levels.put(Skill.FLETCHING, 1);
        levels.put(Skill.SLAYER, 1);
        levels.put(Skill.FARMING, 1);
        levels.put(Skill.CONSTRUCTION, 1);
        levels.put(Skill.HUNTER, 1);
        levels.put(Skill.SAILING, 1);

        return new AccountSnapshot(
                "Membership Test",
                0,
                "Main",
                membershipStatus,
                membershipStatus == MembershipStatus.P2P ? 14 : 0,
                1500,
                0L,
                levels,
                experience
        );
    }
}
