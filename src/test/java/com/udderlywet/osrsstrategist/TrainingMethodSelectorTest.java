package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TrainingMethodSelectorTest
{
    private final TrainingMethodSelector selector =
            new TrainingMethodSelector(
                    new TrainingMethodDatabase()
            );

    @Test
    public void balancedHerbloreReturnsPlan()
    {
        TrainingPlan plan = selector.select(
                p2pData(),
                Skill.HERBLORE,
                1,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME
        );

        assertNotNull(plan);
        assertNotNull(plan.getMethod());
        assertEquals(
                Skill.HERBLORE,
                plan.getMethod().getSkill()
        );

        // The method should never claim herbs are banked until the bank reader
        // has actually observed them. The starter method is therefore phrased
        // around confirmed supplies and remains CHECK_NEEDED.
        assertTrue(
                plan.getMethod().getName().contains("confirmed")
        );
        assertEquals(
                Confidence.CHECK_NEEDED,
                plan.getConfidence()
        );
    }

    @Test
    public void afkMiningPrefersLowAttentionMethodAtThirty()
    {
        TrainingPlan plan = selector.select(
                p2pData(),
                Skill.MINING,
                30,
                StrategyMode.RELAXED,
                SessionIntent.AFK
        );

        assertNotNull(plan);
        assertEquals(
                "Motherlode Mine",
                plan.getMethod().getName()
        );
    }

    private static GameData p2pData()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 60);
            xp.put(skill, Experience.getXpForLevel(60));
        }
        return GameData.builder(new AccountSnapshot(
                "Selector Test",
                0,
                "Main",
                MembershipStatus.P2P,
                1,
                1500,
                0L,
                levels,
                xp)).build();
    }
}
