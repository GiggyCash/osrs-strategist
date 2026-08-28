package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProgressAnalyticsServiceTest
{
    @Test
    public void absoluteXpEventsNeedABaselineAndNeverCreateNegativeProgress()
    {
        ProgressAnalyticsService service = new ProgressAnalyticsService();
        service.reset(1_000L);

        assertFalse(service.record(new StatChanged(
                Skill.MINING, 1_000, 10, 10), 2_000L));
        assertTrue(service.record(new StatChanged(
                Skill.MINING, 1_125, 11, 11), 3_000L));
        assertFalse(service.record(Skill.MINING, 1_000, 10, 4_000L));
        assertFalse(service.record(Skill.MINING, -1, 10, 5_000L));
        assertFalse(service.record(Skill.MINING, 200_000_001, 10, 5_000L));

        SkillSessionProgress progress = service.snapshot(6_000L)
                .getSkills().get(Skill.MINING);
        assertEquals(0, progress.getXpGained());
        assertEquals(10, progress.getCurrentLevel());
    }

    @Test
    public void tracksSessionTotalsLevelsActiveTimeAndMeasuredRate()
    {
        ProgressAnalyticsService service = new ProgressAnalyticsService();
        service.beginSession(account(Skill.FISHING, 10, 1_000), 1_000L);

        service.record(Skill.FISHING, 1_050, 10, 11_000L);
        service.record(Skill.FISHING, 1_150, 11, 26_000L);
        ProgressSessionSnapshot calculating = service.snapshot(26_000L);
        assertFalse(calculating.getSkills().get(Skill.FISHING)
                .getRate().isReady());

        service.record(Skill.FISHING, 1_250, 12, 46_000L);
        ProgressSessionSnapshot ready = service.snapshot(46_000L);

        assertEquals(250L, ready.getTotalXpGained());
        assertEquals(2, ready.getLevelsGained());
        assertEquals(35_000L, ready.getActiveDurationMillis());
        XpRateEstimate rate = ready.getSkills().get(Skill.FISHING).getRate();
        assertTrue(rate.isReady());
        assertEquals(20_571L, rate.getXpPerHour());
    }

    @Test
    public void idleGapIsNotActiveTrainingAndForcesRateToRecalculate()
    {
        ProgressAnalyticsService service = readyService();
        long before = service.snapshot(46_000L).getActiveDurationMillis();

        service.record(Skill.FISHING, 1_300, 12,
                46_000L + ProgressAnalyticsService.IDLE_GAP_MILLIS + 1L);
        ProgressSessionSnapshot after = service.snapshot(
                46_000L + ProgressAnalyticsService.IDLE_GAP_MILLIS + 1L);

        assertEquals(before, after.getActiveDurationMillis());
        assertFalse(after.getSkills().get(Skill.FISHING)
                .getRate().isReady());
    }

    @Test
    public void targetEtaIsSafeAndMethodChangeRebasesMeasuredRate()
    {
        ProgressAnalyticsService service = readyService();
        service.setTarget(new ProgressTarget("skill:fishing", "fly-fishing",
                Skill.FISHING, 11));

        ProgressTargetProjection projection = service.snapshot(46_000L)
                .getTargetProjection();
        assertEquals(ProgressTargetProjection.State.READY,
                projection.getState());
        assertEquals(Math.max(0, Experience.getXpForLevel(11) - 1_250),
                projection.getXpRemaining());
        assertTrue(projection.getEtaMillis() > 0L);

        service.setTarget(new ProgressTarget("skill:fishing", "lobsters",
                Skill.FISHING, 12));
        assertEquals(ProgressTargetProjection.State.CALCULATING,
                service.snapshot(46_000L).getTargetProjection().getState());
    }

    @Test
    public void switchingToAnotherSkillCannotReuseAnUnrelatedOldRate()
    {
        ProgressAnalyticsService service = readyService();
        service.setTarget(new ProgressTarget("skill:fishing", "fly-fishing",
                Skill.FISHING, 11));
        service.record(Skill.MINING, 1_000, 10, 47_000L);
        service.record(Skill.MINING, 1_100, 10, 62_000L);
        service.record(Skill.MINING, 1_200, 10, 82_000L);
        service.setTarget(new ProgressTarget("skill:mining", "iron-ore",
                Skill.MINING, 11));

        assertEquals(ProgressTargetProjection.State.CALCULATING,
                service.snapshot(82_000L).getTargetProjection().getState());
    }

    @Test
    public void completedTargetHasZeroEtaEvenWithoutRateEvidence()
    {
        ProgressAnalyticsService service = new ProgressAnalyticsService();
        int levelTwoXp = Experience.getXpForLevel(2);
        service.beginSession(account(Skill.COOKING, 2, levelTwoXp), 1_000L);
        service.setTarget(new ProgressTarget("skill:cooking", "fish",
                Skill.COOKING, 2));

        ProgressTargetProjection projection = service.snapshot(1_000L)
                .getTargetProjection();
        assertEquals(ProgressTargetProjection.State.COMPLETE,
                projection.getState());
        assertEquals(0L, projection.getEtaMillis());
    }

    @Test
    public void bucketsAndMilestonesRemainBoundedAndDeduplicated()
    {
        ProgressAnalyticsService service = new ProgressAnalyticsService();
        service.beginSession(account(Skill.WOODCUTTING, 1, 0), 0L);
        int xp = 0;
        for (int index = 0; index < 400; index++)
        {
            xp++;
            service.record(Skill.WOODCUTTING, xp, 1,
                    1L + index * ProgressAnalyticsService.BUCKET_MILLIS);
        }
        ProgressMilestone same = new ProgressMilestone("quest:test",
                ProgressMilestoneType.QUEST, "Test quest", null, null, 1L);
        assertTrue(service.recordMilestone(same));
        assertFalse(service.recordMilestone(same));

        ProgressSessionSnapshot snapshot = service.snapshot(
                400L * ProgressAnalyticsService.BUCKET_MILLIS);
        assertEquals(ProgressAnalyticsService.MAX_BUCKETS,
                snapshot.getBuckets().size());
        assertEquals(1, snapshot.getMilestones().size());
    }

    private static ProgressAnalyticsService readyService()
    {
        ProgressAnalyticsService service = new ProgressAnalyticsService();
        service.beginSession(account(Skill.FISHING, 10, 1_000), 1_000L);
        service.record(Skill.FISHING, 1_050, 10, 11_000L);
        service.record(Skill.FISHING, 1_150, 11, 26_000L);
        service.record(Skill.FISHING, 1_250, 12, 46_000L);
        return service;
    }

    private static AccountSnapshot account(
            Skill changed, int level, int xp)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> experience = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            experience.put(skill, 0);
        }
        levels.put(changed, level);
        experience.put(changed, xp);
        return new AccountSnapshot("Tester", 0, "Main", 1, xp,
                levels, experience);
    }
}
