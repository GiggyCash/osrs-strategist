package com.udderlywet.osrsstrategist;

/** Current Slayer task evidence observed from RuneLite/client state. */
public final class SlayerSnapshot
{
    private final String taskName;
    private final int remaining;
    private final String masterName;
    private final String taskLocation;
    private final int points;
    private final RecommendationConfidence confidence;

    /** Compatibility constructor retained for older callers. */
    public SlayerSnapshot(
            String taskName,
            int remaining,
            String masterName,
            int points,
            RecommendationConfidence confidence)
    {
        this(taskName, remaining, masterName, null, points, confidence);
    }

    public SlayerSnapshot(
            String taskName,
            int remaining,
            String masterName,
            String taskLocation,
            int points,
            RecommendationConfidence confidence)
    {
        this.taskName = taskName;
        this.remaining = Math.max(0, remaining);
        this.masterName = masterName;
        this.taskLocation = taskLocation;
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
                null,
                0,
                RecommendationConfidence.CHECK_NEEDED
        );
    }

    public String getTaskName() { return taskName; }
    public int getRemaining() { return remaining; }
    public String getMasterName() { return masterName; }
    public String getTaskLocation() { return taskLocation; }
    public int getPoints() { return points; }
    public RecommendationConfidence getConfidence() { return confidence; }

    public boolean hasTask()
    {
        return taskName != null && !taskName.trim().isEmpty() && remaining > 0;
    }
}
