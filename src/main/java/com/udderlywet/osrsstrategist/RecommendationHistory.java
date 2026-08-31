package com.udderlywet.osrsstrategist;

import java.util.*;

/** Bounded local history so learning cannot grow profile config forever. */
public final class RecommendationHistory
{
    private static final int MAX_ENTRIES = 200;
    private final List<RecommendationHistoryEntry> entries = new ArrayList<>();

    public void add(
            String activityId,
            String title,
            RecommendationHistoryAction action)
    {
        if (activityId == null || action == null) return;
        entries.add(new RecommendationHistoryEntry(
                activityId, title, action, System.currentTimeMillis()));
        trim();
    }

    public void replaceAll(List<RecommendationHistoryEntry> values)
    {
        entries.clear();
        if (values != null) entries.addAll(values);
        trim();
    }

    public void clear()
    {
        entries.clear();
    }

    public List<RecommendationHistoryEntry> snapshot()
    {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    private void trim()
    {
        while (entries.size() > MAX_ENTRIES)
        {
            entries.remove(0);
        }
    }
}
