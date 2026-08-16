package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class HardcoreTrainingSafetyTest
{
    @Test
    public void hardcoreNeverAutoSelectsWildernessTrainingEvenWhenEnabled()
    {
        TrainingMethodSelector selector = new TrainingMethodSelector(
                new TrainingMethodDatabase());
        StrategyDataBundle data = StrategyDataBundle.builder(account(3)).build();

        TrainingPlan plan = selector.select(
                data, Skill.PRAYER, 70,
                StrategyMode.EFFICIENT, SessionIntent.PICK_FOR_ME, true);

        assertNotNull(plan);
        assertNotNull(plan.getMethod());
        assertFalse(plan.getMethod().isWilderness());
    }

    private static AccountSnapshot account(int type)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(
                "Hardcore", type, "Hardcore Ironman",
                MembershipStatus.P2P, 1,
                1600, 0L, levels, xp);
    }
}
