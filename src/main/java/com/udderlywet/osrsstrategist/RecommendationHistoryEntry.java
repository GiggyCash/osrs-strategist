package com.udderlywet.osrsstrategist;

/** One per-character interaction/completion event retained for later learning. */
public final class RecommendationHistoryEntry
{
    private final String activityId;
    private final String title;
    private final RecommendationHistoryAction action;
    private final long occurredAtMillis;

    public RecommendationHistoryEntry(
            String activityId,
            String title,
            RecommendationHistoryAction action,
            long occurredAtMillis)
    {
        this.activityId = activityId;
        this.title = title;
        this.action = action;
        this.occurredAtMillis = occurredAtMillis;
    }

    public String getActivityId() { return activityId; }
    public String getTitle() { return title; }
    public RecommendationHistoryAction getAction() { return action; }
    public long getOccurredAtMillis() { return occurredAtMillis; }
}
