package compass;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;

/**
 * Event-driven local XP/session analytics.
 *
 * <p>RuneLite {@link StatChanged} values are absolute. A first observation or
 * a lower value therefore establishes a baseline and never becomes negative
 * progress. Rates use only nearby positive-XP intervals, become available
 * after multiple timed observations, and reset across idle gaps or method
 * changes. No game-tick polling is required.</p>
 */
@Singleton
public class ProgressAnalyticsService
{
    static final int MAX_SKILL_XP = 200_000_000;
    static final long BUCKET_MILLIS = 5L * 60L * 1000L;
    static final int MAX_BUCKETS = 288;
    static final int MAX_RATE_INTERVALS = 120;
    static final long RATE_WINDOW_MILLIS = 30L * 60L * 1000L;
    static final long IDLE_GAP_MILLIS = 5L * 60L * 1000L;
    static final long MIN_RATE_DURATION_MILLIS = 30_000L;
    static final int MIN_RATE_INTERVALS = 2;
    static final int MAX_SESSION_MILESTONES = 100;

    private final EnumMap<Skill, MutableSkill> skills =
            new EnumMap<>(Skill.class);
    private final Deque<MutableBucket> buckets = new ArrayDeque<>();
    private final List<ProgressMilestone> milestones = new ArrayList<>();
    private final Set<String> milestoneIds = new HashSet<>();
    private long startedAtMillis;
    private long updatedAtMillis;
    private long activeDurationMillis;
    private long lastProgressAtMillis;
    private ProgressTarget target;

    public void beginSession(AccountSnapshot account)
    {
        beginSession(account, System.currentTimeMillis());
    }

    /** Begins or rebases the session from a complete, read-only account view. */
    public synchronized void beginSession(AccountSnapshot account, long nowMillis)
    {
        reset(nowMillis);
        if (account == null) return;
        for (Skill skill : Skill.values())
        {
            if (overall(skill)) continue;
            var xp = account.xp(skill);
            var level = account.level(skill);
            if (validXp(xp) && level > 0)
                skills.put(skill, new MutableSkill(xp, level));
        }
    }

    /** Clears all volatile state, used on login/profile changes. */
    public void reset()
    {
        reset(System.currentTimeMillis());
    }

    public synchronized void reset(long nowMillis)
    {
        skills.clear();
        buckets.clear();
        milestones.clear();
        milestoneIds.clear();
        target = null;
        startedAtMillis = Math.max(0L, nowMillis);
        updatedAtMillis = startedAtMillis;
        activeDurationMillis = 0L;
        lastProgressAtMillis = 0L;
    }

    public boolean record(StatChanged event)
    {
        return record(event, System.currentTimeMillis());
    }

    public boolean record(StatChanged event, long nowMillis)
    {
        return event != null && record(event.getSkill(), event.getXp(),
                event.getLevel(), nowMillis);
    }

    /**
     * Records an absolute RuneLite XP value. Returns true only for positive
     * session progress; baselines, duplicates and invalid observations return
     * false.
     */
    public synchronized boolean record(
            Skill skill, int absoluteXp, int level, long nowMillis)
    {
        if (skill == null || overall(skill) || !validXp(absoluteXp)
                || level <= 0 || nowMillis < startedAtMillis
                || nowMillis < updatedAtMillis)
            return false;

        var state = skills.get(skill);
        if (state == null)
        {
            skills.put(skill, new MutableSkill(absoluteXp, level));
            updatedAtMillis = Math.max(updatedAtMillis, nowMillis);
            return false;
        }
        if (nowMillis < state.lastObservationAtMillis)
            return false;

        var previousXp = state.currentXp;
        state.lastObservationAtMillis = nowMillis;
        state.currentLevel = Math.max(state.currentLevel, level);
        updatedAtMillis = Math.max(updatedAtMillis, nowMillis);

        if (absoluteXp < previousXp)
        {
            // Account resets, stale profile transitions and RuneLite rebases
            // must never retain another account's chart, milestones, target,
            // or active-time totals. The next complete account read will fill
            // the other skill baselines again.
            reset(nowMillis);
            skills.put(skill, new MutableSkill(absoluteXp, level));
            return false;
        }
        if (absoluteXp == previousXp) return false;

        var gained = absoluteXp - previousXp;
        state.currentXp = absoluteXp;
        addActiveTime(nowMillis);
        addRateInterval(state, gained, nowMillis);
        addBucket(skill, gained, nowMillis);
        return true;
    }

    /** Updates the active plan checkpoint and rebases incompatible method rates. */
    public synchronized boolean setTarget(ProgressTarget next)
    {
        var changed = !sameTarget(target, next);
        if (!changed) return false;
        if (target != null && next != null
                && (target.getSkill() != next.getSkill()
                || !Objects.equals(target.getMethodId(), next.getMethodId())))
        {
            var state = skills.get(next.getSkill());
            if (state != null) state.rateIntervals.clear();
        }
        target = next;
        return true;
    }

    public synchronized void clearTarget()
    {
        target = null;
    }

    /** Ends the current active/rate segment without discarding session gains. */
    public synchronized void pause(long nowMillis)
    {
        updatedAtMillis = Math.max(updatedAtMillis,
                Math.max(startedAtMillis, nowMillis));
        lastProgressAtMillis = 0L;
        for (MutableSkill state : skills.values())
        {
            state.lastProgressAtMillis = 0L;
            state.rateIntervals.clear();
        }
    }

    /** Adds a typed non-XP milestone once per session. */
    public synchronized boolean recordMilestone(ProgressMilestone milestone)
    {
        if (milestone == null || !milestoneIds.add(milestone.getId()))
            return false;
        milestones.add(milestone);
        while (milestones.size() > MAX_SESSION_MILESTONES)
        {
            var removed = milestones.remove(0);
            milestoneIds.remove(removed.getId());
        }
        updatedAtMillis = Math.max(updatedAtMillis,
                milestone.getOccurredAtMillis());
        return true;
    }

    public synchronized ProgressSessionSnapshot snapshot(long nowMillis)
    {
        long effectiveNow = Math.max(updatedAtMillis,
                Math.max(startedAtMillis, nowMillis));
        EnumMap<Skill, SkillSessionProgress> result =
                new EnumMap<>(Skill.class);
        for (Map.Entry<Skill, MutableSkill> entry : skills.entrySet())
        {
            var value = entry.getValue();
            result.put(entry.getKey(), new SkillSessionProgress(
                    entry.getKey(), value.startingXp, value.currentXp,
                    value.startingLevel, value.currentLevel,
                    rateFor(value, effectiveNow)));
        }

        List<ProgressTimeBucket> bucketCopy = new ArrayList<>();
        for (MutableBucket value : buckets) bucketCopy.add(value.snapshot());
        return new ProgressSessionSnapshot(startedAtMillis, effectiveNow,
                activeDurationMillis, result, bucketCopy, milestones,
                targetProjection(result));
    }

    public ProgressSessionSnapshot snapshot()
    {
        return snapshot(System.currentTimeMillis());
    }

    private void addActiveTime(long nowMillis)
    {
        if (lastProgressAtMillis > 0L)
        {
            var gap = nowMillis - lastProgressAtMillis;
            if (gap >= 0L && gap <= IDLE_GAP_MILLIS)
                activeDurationMillis += gap;
        }
        lastProgressAtMillis = nowMillis;
    }

    private static void addRateInterval(
            MutableSkill state, int gained, long nowMillis)
    {
        if (state.lastProgressAtMillis > 0L)
        {
            var gap = nowMillis - state.lastProgressAtMillis;
            if (gap > IDLE_GAP_MILLIS)
                state.rateIntervals.clear();
            else if (gap > 0L)
                state.rateIntervals.addLast(new RateInterval(
                        gained, gap, nowMillis));
        }
        state.lastProgressAtMillis = nowMillis;
        trimRate(state.rateIntervals, nowMillis);
    }

    private static void trimRate(Deque<RateInterval> intervals, long nowMillis)
    {
        while (intervals.size() > MAX_RATE_INTERVALS
                || (!intervals.isEmpty()
                && nowMillis - intervals.peekFirst().endedAtMillis
                > RATE_WINDOW_MILLIS))
            intervals.removeFirst();
    }

    private void addBucket(Skill skill, int gained, long nowMillis)
    {
        var start = nowMillis - Math.floorMod(nowMillis, BUCKET_MILLIS);
        var bucket = buckets.peekLast();
        if (bucket == null || bucket.startedAtMillis != start)
        {
            bucket = new MutableBucket(start);
            buckets.addLast(bucket);
        }
        bucket.add(skill, gained);
        while (buckets.size() > MAX_BUCKETS) buckets.removeFirst();
    }

    private static XpRateEstimate rateFor(MutableSkill state, long nowMillis)
    {
        trimRate(state.rateIntervals, nowMillis);
        var xp = 0L;
        var millis = 0L;
        for (RateInterval interval : state.rateIntervals)
        {
            xp += interval.xp;
            millis += interval.activeMillis;
        }
        if (state.rateIntervals.size() < MIN_RATE_INTERVALS
                || millis < MIN_RATE_DURATION_MILLIS || xp <= 0L)
            return XpRateEstimate.calculating(state.rateIntervals.size());
        var hourly = Math.round(xp * 3_600_000.0 / millis);
        return hourly <= 0L
                ? XpRateEstimate.calculating(state.rateIntervals.size())
                : XpRateEstimate.ready(hourly, state.rateIntervals.size());
    }

    private ProgressTargetProjection targetProjection(
            Map<Skill, SkillSessionProgress> progress)
    {
        if (target == null) return ProgressTargetProjection.noTarget();
        var skill = progress.get(target.getSkill());
        var currentXp = skill == null ? 0 : skill.getCurrentXp();
        var remaining = Math.max(0, target.getTargetXp() - currentXp);
        if (remaining == 0) return ProgressTargetProjection.complete(target);
        if (skill == null || !skill.getRate().isReady())
            return ProgressTargetProjection.calculating(target, remaining);
        var rate = skill.getRate().getXpPerHour();
        if (rate <= 0L)
            return ProgressTargetProjection.calculating(target, remaining);
        var eta = remaining * 3_600_000.0 / rate;
        long etaMillis = eta >= Long.MAX_VALUE
                ? Long.MAX_VALUE : Math.max(1L, Math.round(eta));
        return ProgressTargetProjection.ready(target, remaining, etaMillis);
    }

    private static boolean sameTarget(ProgressTarget first, ProgressTarget second)
    {
        if (first == null || second == null) return first == second;
        return Objects.equals(first.getActivityId(), second.getActivityId())
                && Objects.equals(first.getMethodId(), second.getMethodId())
                && first.getSkill() == second.getSkill()
                && first.getTargetLevel() == second.getTargetLevel();
    }

    private static boolean validXp(int value)
    {
        return value >= 0 && value <= MAX_SKILL_XP;
    }

    private static boolean overall(Skill skill)
    {
        return skill != null && "OVERALL".equals(skill.name());
    }

    private static final class MutableSkill
    {
        private int startingXp;
        private int currentXp;
        private int startingLevel;
        private int currentLevel;
        private long lastObservationAtMillis;
        private long lastProgressAtMillis;
        private final Deque<RateInterval> rateIntervals = new ArrayDeque<>();

        private MutableSkill(int xp, int level)
        {
            startingXp = xp;
            currentXp = xp;
            startingLevel = level;
            currentLevel = level;
        }

    }

    private static final class RateInterval
    {
        private final int xp;
        private final long activeMillis;
        private final long endedAtMillis;

        private RateInterval(int xp, long activeMillis, long endedAtMillis)
        {
            this.xp = xp;
            this.activeMillis = activeMillis;
            this.endedAtMillis = endedAtMillis;
        }
    }

    private static final class MutableBucket
    {
        private final long startedAtMillis;
        private final EnumMap<Skill, Integer> xp = new EnumMap<>(Skill.class);

        private MutableBucket(long startedAtMillis)
        {
            this.startedAtMillis = startedAtMillis;
        }

        private void add(Skill skill, int value)
        {
            xp.merge(skill, value, (first, second) -> {
                var total = (long) first + second;
                return (int) Math.min(Integer.MAX_VALUE, total);
            });
        }

        private ProgressTimeBucket snapshot()
        {
            return new ProgressTimeBucket(startedAtMillis, xp);
        }
    }
}
