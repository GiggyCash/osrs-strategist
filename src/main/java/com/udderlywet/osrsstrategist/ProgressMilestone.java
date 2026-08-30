package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One meaningful account progression event suitable for a session recap. */
public final class ProgressMilestone
{
    @Getter
    private final String id;
    @Getter
    private final ProgressMilestoneType type;
    @Getter
    private final String title;
    @Getter
    private final String detail;
    @Getter
    private final String goalId;
    @Getter
    private final long occurredAtMillis;

    public ProgressMilestone(
            String id,
            ProgressMilestoneType type,
            String title,
            String detail,
            String goalId,
            long occurredAtMillis)
    {
        if (id == null || id.trim().isEmpty() || type == null
                || title == null || title.trim().isEmpty())
        {
            throw new IllegalArgumentException("Milestone needs identity and type");
        }
        this.id = id;
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.goalId = goalId;
        this.occurredAtMillis = Math.max(0L, occurredAtMillis);
    }

}
