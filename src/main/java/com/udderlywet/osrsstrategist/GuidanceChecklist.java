package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public String getActivityId() { return activityId; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public List<GuidanceStep> getSteps() { return steps; }
    public String getBring() { return bring; }
    public String getWhere() { return where; }
    public String getAction() { return action; }
    public String getProgress() { return progress; }
    public String getImportant() { return important; }

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
