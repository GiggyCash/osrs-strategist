package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Recurring reminders must not activate from a skill level alone.
 */
public class OpportunityEngineTest
{
    private final OpportunityEngine engine = new OpportunityEngine();

    @Test
    public void highSkillsDoNotCreateUnobservedReminders()
    {
        StrategyDataBundle data =
                StrategyDataBundle.builder(account(50, 50)).build();

        assertTrue(engine.evaluate(data).isEmpty());
    }

    @Test
    public void observedBirdhouseTimerCreatesOpportunity()
    {
        Map<String, Long> timers = new HashMap<>();
        timers.put(
                "opportunity:birdhouse",
                System.currentTimeMillis() - 1L
        );

        StrategyDataBundle data =
                StrategyDataBundle.builder(account(50, 50))
                        .recurringOpportunities(
                                new RecurringOpportunitySnapshot(timers)
                        )
                        .build();

        List<Opportunity> opportunities = engine.evaluate(data);

        assertEquals(1, opportunities.size());
        assertEquals(
                OpportunityType.BIRDHOUSE_RUN,
                opportunities.get(0).getType()
        );
        assertTrue(opportunities.get(0).isReady());
    }

    private static AccountSnapshot account(
            int hunter,
            int farming)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> experience = new EnumMap<>(Skill.class);

        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            experience.put(skill, 0);
        }

        levels.put(Skill.HUNTER, hunter);
        levels.put(Skill.FARMING, farming);

        return new AccountSnapshot(
                "Test",
                0,
                "Main",
                MembershipStatus.P2P,
                1,
                24,
                0L,
                levels,
                experience
        );
    }
}
