package com.udderlywet.osrsstrategist;

/**
 * Current Slayer state. This is intentionally small: task selection, Slayer
 * master routing, block lists, unlock purchases, and task-specific loadouts can
 * all build on top of this later without changing the recommendation contract.
 */
public final class SlayerSnapshot
{
    private final String taskName;
    private final int remaining;
    private final String masterName;
    private final int points;
    private final RecommendationConfidence confidence;

    public SlayerSnapshot(
            String taskName,
            int remaining,
            String masterName,
            int points,
            RecommendationConfidence confidence)
    {
        this.taskName = taskName;
        this.remaining = Math.max(0, remaining);
        this.masterName = masterName;
        this.points = Math.max(0, points);
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED
                : confidence;
    }

    public static SlayerSnapshot unknown()
    {
        return new SlayerSnapshot(
                null,
                0,
                null,
                0,
                RecommendationConfidence.CHECK_NEEDED
        );
    }

    public String getTaskName() { return taskName; }
    public int getRemaining() { return remaining; }
    public String getMasterName() { return masterName; }
    public int getPoints() { return points; }
    public RecommendationConfidence getConfidence() { return confidence; }

    public boolean hasTask()
    {
        return taskName != null && !taskName.trim().isEmpty() && remaining > 0;
    }
}
