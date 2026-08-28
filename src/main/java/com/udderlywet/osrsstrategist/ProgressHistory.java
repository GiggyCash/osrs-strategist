package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Character-local, strictly bounded progress archive. */
public final class ProgressHistory
{
    static final int MAX_SESSIONS = 30;
    static final int MAX_MILESTONES = 100;
    static final int MAX_BUCKETS = 288;

    private final List<ProgressSessionSummary> sessions = new ArrayList<>();
    private final List<ProgressMilestone> milestones = new ArrayList<>();
    private final List<ProgressTimeBucket> buckets = new ArrayList<>();

    public void archive(ProgressSessionSnapshot snapshot)
    {
        if (snapshot == null) return;
        if (snapshot.getTotalXpGained() > 0 || snapshot.getLevelsGained() > 0
                || !snapshot.getMilestones().isEmpty())
            sessions.add(new ProgressSessionSummary(snapshot));
        milestones.addAll(snapshot.getMilestones());
        buckets.addAll(snapshot.getBuckets());
        trim();
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
        trim();
    }

    public List<ProgressSessionSummary> getSessions()
    {
        return Collections.unmodifiableList(new ArrayList<>(sessions));
    }

    public List<ProgressMilestone> getMilestones()
    {
        return Collections.unmodifiableList(new ArrayList<>(milestones));
    }

    public List<ProgressTimeBucket> getBuckets()
    {
        return Collections.unmodifiableList(new ArrayList<>(buckets));
    }

    public void clear()
    {
        sessions.clear();
        milestones.clear();
        buckets.clear();
    }

    private void trim()
    {
        trimOldest(sessions, MAX_SESSIONS);
        trimOldest(milestones, MAX_MILESTONES);
        trimOldest(buckets, MAX_BUCKETS);
    }

    private static void trimOldest(List<?> values, int limit)
    {
        while (values.size() > limit) values.remove(0);
    }
}
