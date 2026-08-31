package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MilestoneTrackerTest
{
    private final MilestoneTracker tracker = new MilestoneTracker();

    @Test
    public void doesNotCompleteBeforeTarget()
    {
        TrackedMilestone tracked = new TrackedMilestone(
                "skill:farming",
                "Train Farming to 10",
                Skill.FARMING.name(),
                1,
                10
        );

        assertNull(
                tracker.detectCompletion(
                        tracked,
                        accountWithFarming(9)
                )
        );
    }

    @Test
    public void completesWithoutAnyButtonPressAtTarget()
    {
        TrackedMilestone tracked = new TrackedMilestone(
                "skill:farming",
                "Train Farming to 10",
                Skill.FARMING.name(),
                1,
                10
        );

        MilestoneCompletion completion = tracker.detectCompletion(
                tracked,
                accountWithFarming(10)
        );

        assertNotNull(completion);
        assertEquals(Skill.FARMING, completion.getSkill());
        assertEquals(1, completion.getStartedAtLevel());
        assertEquals(10, completion.getTargetLevel());
    }

    @Test
    public void createsTrackedCheckpointFromTopRecommendation()
    {
        Recommendation recommendation = new Recommendation(
                "skill:farming",
                "Train Farming to 10",
                "Useful progression.",
                50.0,
                null,
                Confidence.CHECK_NEEDED,
                1,
                10
        );

        TrackedMilestone tracked = tracker.fromRecommendations(
                Collections.singletonList(recommendation)
        );

        assertNotNull(tracked);
        assertEquals("skill:farming", tracked.getActivityId());
        assertEquals(Skill.FARMING, tracked.getSkill());
        assertEquals(10, tracked.getTargetLevel());
    }

    private static AccountSnapshot accountWithFarming(int farmingLevel)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> experience = new EnumMap<>(Skill.class);

        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            experience.put(skill, 0);
        }

        levels.put(Skill.FARMING, farmingLevel);

        return new AccountSnapshot(
                "Test",
                0,
                "Main",
                100,
                0L,
                levels,
                experience
        );
    }
}
