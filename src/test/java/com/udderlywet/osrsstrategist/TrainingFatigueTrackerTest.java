package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrainingFatigueTrackerTest
{
    @Test
    public void balancedOnlyPenalizesSustainedTraining()
    {
        TrainingFatigueTracker tracker = new TrainingFatigueTracker();
        long start = 1_000_000L;
        assertFalse(tracker.record(Skill.MINING, 1000,
                StrategyMode.BALANCED, start).isPresent());
        for (int minute = 8; minute <= 40; minute += 8)
        {
            assertFalse(tracker.record(
                    Skill.MINING,
                    1000 + minute * 100,
                    StrategyMode.BALANCED,
                    start + minutes(minute)).isPresent());
        }

        TrainingFatigueTracker.FatigueSignal signal = tracker.record(
                Skill.MINING, 6000, StrategyMode.BALANCED,
                start + minutes(46));
        assertTrue(signal.isPresent());
        assertEquals("skill:mining", signal.getActivityId());
        assertEquals(-8.0, signal.getScoreDelta(), 0.001);
    }

    @Test
    public void relaxedRotatesEarlierAndMoreStrongly()
    {
        TrainingFatigueTracker tracker = new TrainingFatigueTracker();
        long start = 5_000_000L;
        tracker.record(Skill.WOODCUTTING, 1000, StrategyMode.RELAXED, start);
        tracker.record(Skill.WOODCUTTING, 1800, StrategyMode.RELAXED,
                start + minutes(8));
        tracker.record(Skill.WOODCUTTING, 2600, StrategyMode.RELAXED,
                start + minutes(16));
        tracker.record(Skill.WOODCUTTING, 3400, StrategyMode.RELAXED,
                start + minutes(24));

        TrainingFatigueTracker.FatigueSignal signal = tracker.record(
                Skill.WOODCUTTING, 4100, StrategyMode.RELAXED,
                start + minutes(31));

        assertTrue(signal.isPresent());
        assertEquals(-14.0, signal.getScoreDelta(), 0.001);
    }

    @Test
    public void efficientNeverAppliesPsychologicalVarietyPenalty()
    {
        TrainingFatigueTracker tracker = new TrainingFatigueTracker();
        long start = 10_000_000L;
        tracker.record(Skill.AGILITY, 1000, StrategyMode.EFFICIENT, start);
        for (int minute = 8; minute <= 120; minute += 8)
        {
            assertFalse(tracker.record(
                    Skill.AGILITY,
                    1000 + minute * 100,
                    StrategyMode.EFFICIENT,
                    start + minutes(minute)).isPresent());
        }
    }

    @Test
    public void switchingSkillsResetsContinuity()
    {
        TrainingFatigueTracker tracker = new TrainingFatigueTracker();
        long start = 20_000_000L;
        tracker.record(Skill.FISHING, 1000, StrategyMode.RELAXED, start);
        tracker.record(Skill.FISHING, 1800, StrategyMode.RELAXED,
                start + minutes(8));
        tracker.record(Skill.FISHING, 2600, StrategyMode.RELAXED,
                start + minutes(16));

        assertFalse(tracker.record(Skill.COOKING, 1000,
                StrategyMode.RELAXED,
                start + minutes(17)).isPresent());
        assertFalse(tracker.record(Skill.COOKING, 1800,
                StrategyMode.RELAXED,
                start + minutes(25)).isPresent());
        assertFalse(tracker.record(Skill.COOKING, 2600,
                StrategyMode.RELAXED,
                start + minutes(33)).isPresent());
        assertFalse(tracker.record(Skill.COOKING, 3400,
                StrategyMode.RELAXED,
                start + minutes(41)).isPresent());
    }

    @Test
    public void hitpointsDoesNotBreakChosenCombatSkillContinuity()
    {
        TrainingFatigueTracker tracker = new TrainingFatigueTracker();
        long start = 30_000_000L;
        tracker.record(Skill.DEFENCE, 1000, StrategyMode.RELAXED, start);
        tracker.record(Skill.HITPOINTS, 5000, StrategyMode.RELAXED,
                start + minutes(4));
        tracker.record(Skill.DEFENCE, 1800, StrategyMode.RELAXED,
                start + minutes(8));
        tracker.record(Skill.HITPOINTS, 5600, StrategyMode.RELAXED,
                start + minutes(12));
        tracker.record(Skill.DEFENCE, 2600, StrategyMode.RELAXED,
                start + minutes(16));
        tracker.record(Skill.DEFENCE, 3400, StrategyMode.RELAXED,
                start + minutes(24));

        TrainingFatigueTracker.FatigueSignal signal = tracker.record(
                Skill.DEFENCE, 4100, StrategyMode.RELAXED,
                start + minutes(31));
        assertTrue(signal.isPresent());
        assertEquals("skill:defence", signal.getActivityId());
    }

    @Test
    public void longBreakStartsFreshSession()
    {
        TrainingFatigueTracker tracker = new TrainingFatigueTracker();
        long start = 40_000_000L;
        tracker.record(Skill.CRAFTING, 1000, StrategyMode.RELAXED, start);
        assertFalse(tracker.record(Skill.CRAFTING, 3000,
                StrategyMode.RELAXED,
                start + minutes(11)).isPresent());
        assertFalse(tracker.record(Skill.CRAFTING, 4000,
                StrategyMode.RELAXED,
                start + minutes(20)).isPresent());
    }

    private static long minutes(int value)
    {
        return value * 60L * 1000L;
    }
}
