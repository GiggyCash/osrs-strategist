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
        assertFalse(tracker.record(Skill.MINING, 2000,
                StrategyMode.BALANCED,
                start + 20L * 60L * 1000L).isPresent());

        TrainingFatigueTracker.FatigueSignal signal = tracker.record(
                Skill.MINING, 3000, StrategyMode.BALANCED,
                start + 46L * 60L * 1000L);
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
        TrainingFatigueTracker.FatigueSignal signal = tracker.record(
                Skill.WOODCUTTING, 3000, StrategyMode.RELAXED,
                start + 31L * 60L * 1000L);

        assertTrue(signal.isPresent());
        assertEquals(-14.0, signal.getScoreDelta(), 0.001);
    }

    @Test
    public void efficientNeverAppliesPsychologicalVarietyPenalty()
    {
        TrainingFatigueTracker tracker = new TrainingFatigueTracker();
        long start = 10_000_000L;
        tracker.record(Skill.AGILITY, 1000, StrategyMode.EFFICIENT, start);
        assertFalse(tracker.record(
                Skill.AGILITY, 100000, StrategyMode.EFFICIENT,
                start + 3L * 60L * 60L * 1000L).isPresent());
    }

    @Test
    public void switchingSkillsResetsContinuity()
    {
        TrainingFatigueTracker tracker = new TrainingFatigueTracker();
        long start = 20_000_000L;
        tracker.record(Skill.FISHING, 1000, StrategyMode.RELAXED, start);
        tracker.record(Skill.FISHING, 2000, StrategyMode.RELAXED,
                start + 20L * 60L * 1000L);
        assertFalse(tracker.record(Skill.COOKING, 1000,
                StrategyMode.RELAXED,
                start + 21L * 60L * 1000L).isPresent());
        assertFalse(tracker.record(Skill.COOKING, 2000,
                StrategyMode.RELAXED,
                start + 40L * 60L * 1000L).isPresent());
    }

    @Test
    public void hitpointsDoesNotBreakChosenCombatSkillContinuity()
    {
        TrainingFatigueTracker tracker = new TrainingFatigueTracker();
        long start = 30_000_000L;
        tracker.record(Skill.DEFENCE, 1000, StrategyMode.RELAXED, start);
        tracker.record(Skill.HITPOINTS, 5000, StrategyMode.RELAXED,
                start + 10L * 60L * 1000L);
        TrainingFatigueTracker.FatigueSignal signal = tracker.record(
                Skill.DEFENCE, 4000, StrategyMode.RELAXED,
                start + 31L * 60L * 1000L);
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
                start + 11L * 60L * 1000L).isPresent());
        assertFalse(tracker.record(Skill.CRAFTING, 4000,
                StrategyMode.RELAXED,
                start + 20L * 60L * 1000L).isPresent());
    }
}
