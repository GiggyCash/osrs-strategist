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

    public GuidanceChecklist(
            String activityId,
            String title,
            String subtitle,
            List<GuidanceStep> steps)
    {
        this.activityId = activityId;
        this.title = title;
        this.subtitle = subtitle;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public String getActivityId() { return activityId; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public List<GuidanceStep> getSteps() { return steps; }

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
