package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

@Getter
public final class GuidanceChecklist
{
    private final String activityId;
    private final String title;
    private final String subtitle;
    private final List<GuidanceStep> steps;
    private final String bring;
    private final String where;
    private final String action;
    private final String progress;
    private final String important;

    public GuidanceChecklist(
            String activityId,
            String title,
            String subtitle,
            List<GuidanceStep> steps)
    {
        this(activityId, title, subtitle, steps, null, null, null, null, null);
    }

    public GuidanceChecklist(
            String activityId, String title, String subtitle,
            List<GuidanceStep> steps, String bring, String where,
            String action, String progress, String important)
    {
        this.activityId = activityId;
        this.title = title;
        this.subtitle = subtitle;
        this.steps = Collections.unmodifiableList(new ArrayList<>(
                steps == null ? Collections.emptyList() : steps));
        this.bring = bring;
        this.where = where;
        this.action = action;
        this.progress = progress;
        this.important = important;
    }


    public int completeCount()
    {
        var count = 0;
        for (GuidanceStep step : steps) if (step.isComplete()) count++;
        return count;
    }

    public GuidanceStep firstPending()
    {
        for (GuidanceStep step : steps)
        {
            if (!step.isComplete()) return step;
        }
        return null;
    }
}
