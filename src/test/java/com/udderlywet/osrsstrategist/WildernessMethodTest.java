package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WildernessMethodTest
{
    private final TrainingMethodSelector selector =
            new TrainingMethodSelector(new TrainingMethodDatabase());

    @Test
    public void wildernessMethodsAreHardFilteredUnlessEnabled()
    {
        TrainingPlan disabled = selector.select(
                null, Skill.AGILITY, 52, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, false);
        assertEquals("agility_rooftop", disabled.getMethod().getId());

        TrainingPlan enabled = selector.select(
                null, Skill.AGILITY, 52, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, true);
        assertEquals("agility_wilderness", enabled.getMethod().getId());
    }
}
