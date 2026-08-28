package com.udderlywet.osrsstrategist;

/** One meaningful account progression event suitable for a session recap. */
public final class ProgressMilestone
{
    private final String id;
    private final ProgressMilestoneType type;
    private final String title;
    private final String detail;
    private final String goalId;
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

    public String getId() { return id; }
    public ProgressMilestoneType getType() { return type; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public String getGoalId() { return goalId; }
    public long getOccurredAtMillis() { return occurredAtMillis; }
}
