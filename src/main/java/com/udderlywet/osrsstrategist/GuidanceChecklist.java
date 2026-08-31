package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

public final class GuidanceChecklist
{
    @Getter
    private final String activityId;
    @Getter
    private final String title;
    @Getter
    private final String subtitle;
    @Getter
    private final List<GuidanceStep> steps;
    @Getter
    private final String bring;
    @Getter
    private final String where;
    @Getter
    private final String action;
    @Getter
    private final String progress;
    @Getter
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
        int count = 0;
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
