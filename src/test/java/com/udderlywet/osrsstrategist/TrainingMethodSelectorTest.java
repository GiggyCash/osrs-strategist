package com.udderlywet.osrsstrategist;

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
        assertTrue(
                plan.getMethod().getName().contains("banked")
        );
    }

    @Test
    public void afkMiningPrefersLowAttentionMethodAtThirty()
    {
        TrainingPlan plan = selector.select(
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
}
