package compass;
import static java.lang.Math.*;
import static java.util.Collections.*;

import java.util.*;
import net.runelite.api.Skill;

/** Character-local, strictly bounded progress archive. */
public final class ProgressHistory
{
    static final int MAX_SESSIONS = 30;
    static final int MAX_MILESTONES = 100;
    static final int MAX_BUCKETS = 288;

    final List<ProgressSessionSummary> sessions = new ArrayList<>();
    final List<ProgressMilestone> milestones = new ArrayList<>();
    final List<ProgressTimeBucket> buckets = new ArrayList<>();

    public void archive(ProgressSessionSnapshot snapshot)
    {
        if (snapshot == null) return;
        if (snapshot.getTotalXpGained() > 0 || snapshot.getLevelsGained() > 0
                || !snapshot.milestones.isEmpty())
            sessions.add(new ProgressSessionSummary(snapshot));
        milestones.addAll(snapshot.milestones);
        buckets.addAll(snapshot.getBuckets());
        normalizeAndTrim();
    }

    /** Returns a checkpoint document without closing the in-memory session. */
    public ProgressHistory checkpoint(ProgressSessionSnapshot snapshot)
    {
        var copy = new ProgressHistory();
        copy.replaceAll(sessions, milestones, buckets);
        copy.archive(snapshot);
        return copy;
    }

    void replaceAll(
            List<ProgressSessionSummary> nextSessions,
            List<ProgressMilestone> nextMilestones,
            List<ProgressTimeBucket> nextBuckets)
    {
        sessions.clear();
        milestones.clear();
        buckets.clear();
        if (nextSessions != null) sessions.addAll(nextSessions);
        if (nextMilestones != null) milestones.addAll(nextMilestones);
        if (nextBuckets != null) buckets.addAll(nextBuckets);
        normalizeAndTrim();
    }

    public List<ProgressSessionSummary> getSessions()
    {
        return unmodifiableList(new ArrayList<>(sessions));
    }

    public List<ProgressMilestone> getMilestones()
    {
        return unmodifiableList(new ArrayList<>(milestones));
    }

    public List<ProgressTimeBucket> getBuckets()
    {
        return unmodifiableList(new ArrayList<>(buckets));
    }

    public void clear()
    {
        sessions.clear();
        milestones.clear();
        buckets.clear();
    }

    void normalizeAndTrim()
    {
        sessions.sort(Comparator.comparingLong(
                ProgressSessionSummary::getStartedAtMillis).thenComparingLong(
                ProgressSessionSummary::getEndedAtMillis));
        Map<String, ProgressSessionSummary> uniqueSessions =
                new LinkedHashMap<>();
        for (ProgressSessionSummary session : sessions)
            uniqueSessions.putIfAbsent(sessionKey(session), session);
        sessions.clear();
        sessions.addAll(uniqueSessions.values());

        milestones.sort(Comparator.comparingLong(
                ProgressMilestone::getOccurredAtMillis));
        Map<String, ProgressMilestone> uniqueMilestones = new LinkedHashMap<>();
        for (ProgressMilestone milestone : milestones)
            uniqueMilestones.putIfAbsent(milestone.id, milestone);
        milestones.clear();
        milestones.addAll(uniqueMilestones.values());

        TreeMap<Long, EnumMap<Skill, Integer>> mergedBuckets = new TreeMap<>();
        for (ProgressTimeBucket bucket : buckets)
        {
            EnumMap<Skill, Integer> merged = mergedBuckets.computeIfAbsent(
                    bucket.startedAtMillis, ignored ->
                            new EnumMap<>(Skill.class));
            for (Map.Entry<Skill, Integer> entry
                    : bucket.getXpBySkill().entrySet())
                merged.merge(entry.getKey(), max(0, entry.getValue()),
                        ProgressHistory::saturatingAdd);
        }
        buckets.clear();
        for (Map.Entry<Long, EnumMap<Skill, Integer>> entry
                : mergedBuckets.entrySet())
            buckets.add(new ProgressTimeBucket(entry.getKey(), entry.getValue()));

        trimOldest(sessions, MAX_SESSIONS);
        trimOldest(milestones, MAX_MILESTONES);
        trimOldest(buckets, MAX_BUCKETS);
    }

    static String sessionKey(ProgressSessionSummary value)
    {
        return value.startedAtMillis + ":" + value.getEndedAtMillis()
                + ":" + value.activeDurationMillis + ":"
                + value.getTotalXpGained() + ":" + value.getLevelsGained()
                + ":" + value.getXpBySkill() + ":" + milestoneIds(value);
    }

    static String milestoneIds(ProgressSessionSummary value)
    {
        var result = new StringBuilder();
        for (ProgressMilestone milestone : value.milestones)
            result.append(milestone.id).append('|');
        return result.toString();
    }

    static int saturatingAdd(int first, int second)
    {
        var result = (long) first + second;
        return (int) min(Integer.MAX_VALUE, max(0L, result));
    }

    static void trimOldest(List<?> values, int limit)
    {
        while (values.size() > limit) values.remove(0);
    }
}
