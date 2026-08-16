package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExpandedOpportunityEngineTest
{
    @Test
    public void extraDailyContentOnlyAppearsAfterItsTimerWasObserved()
    {
        Map<String, Long> timers = new HashMap<>();
        timers.put("opportunity:battlestaves", 0L);
        timers.put("opportunity:dynamite", 0L);
        StrategyDataBundle data = StrategyDataBundle.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .build();

        java.util.List<Opportunity> result = new OpportunityEngine().evaluate(data);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(
                value -> value.getType() == OpportunityType.BATTLESTAVES));
        assertTrue(result.stream().anyMatch(
                value -> value.getType() == OpportunityType.DYNAMITE));
    }

    @Test
    public void unobservedDailyContentDoesNotInventReminder()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account())
                .recurringOpportunities(RecurringOpportunitySnapshot.unknown())
                .build();
        assertTrue(new OpportunityEngine().evaluate(data).isEmpty());
    }

    private static AccountSnapshot account()
    {
        Map<Skill, Integer> levels = new java.util.EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new java.util.EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(
                "Test", 0, "Main", MembershipStatus.P2P,
                1, 1, 0L, levels, xp);
    }
}
