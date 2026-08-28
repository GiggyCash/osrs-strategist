package com.udderlywet.osrsstrategist;

/** Current Slayer task and point-economy evidence observed from RuneLite. */
public final class SlayerSnapshot
{
    private final String taskName;
    private final int remaining;
    private final String masterName;
    private final String taskLocation;
    private final int points;
    private final Integer taskStreak;
    private final Integer questPoints;
    private final Integer blockSlotCapacity;
    private final Integer occupiedBlockSlots;
    private final RecommendationConfidence confidence;

    /** Compatibility constructor retained for older callers. */
    public SlayerSnapshot(
            String taskName,
            int remaining,
            String masterName,
            int points,
            RecommendationConfidence confidence)
    {
        this(taskName, remaining, masterName, null, points,
                null, null, null, null, confidence);
    }

    public SlayerSnapshot(
            String taskName,
            int remaining,
            String masterName,
            String taskLocation,
            int points,
            RecommendationConfidence confidence)
    {
        this(taskName, remaining, masterName, taskLocation, points,
                null, null, null, null, confidence);
    }

    public SlayerSnapshot(
            String taskName,
            int remaining,
            String masterName,
            String taskLocation,
            int points,
            Integer taskStreak,
            Integer questPoints,
            Integer blockSlotCapacity,
            Integer occupiedBlockSlots,
            RecommendationConfidence confidence)
    {
        this.taskName = taskName;
        this.remaining = Math.max(0, remaining);
        this.masterName = masterName;
        this.taskLocation = taskLocation;
        this.points = Math.max(0, points);
        this.taskStreak = nonNegative(taskStreak);
        this.questPoints = nonNegative(questPoints);
        this.blockSlotCapacity = nonNegative(blockSlotCapacity);
        this.occupiedBlockSlots = nonNegative(occupiedBlockSlots);
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
                null,
                null,
                null,
                null,
                RecommendationConfidence.CHECK_NEEDED
        );
    }

    public String getTaskName() { return taskName; }
    public int getRemaining() { return remaining; }
    public String getMasterName() { return masterName; }
    public String getTaskLocation() { return taskLocation; }
    public int getPoints() { return points; }
    public Integer getTaskStreak() { return taskStreak; }
    public Integer getQuestPoints() { return questPoints; }
    public Integer getBlockSlotCapacity() { return blockSlotCapacity; }
    public Integer getOccupiedBlockSlots() { return occupiedBlockSlots; }
    public RecommendationConfidence getConfidence() { return confidence; }

    public boolean hasTask()
    {
        return taskName != null && !taskName.trim().isEmpty() && remaining > 0;
    }

    public SlayerAssignmentState getAssignmentState()
    {
        if (hasTask()) return SlayerAssignmentState.ASSIGNED;
        if (confidence == RecommendationConfidence.VERIFIED && remaining == 0)
            return SlayerAssignmentState.NO_TASK;
        return SlayerAssignmentState.UNKNOWN;
    }

    /** True only when both capacity and current per-master usage are known. */
    public boolean hasKnownFreeBlockSlot()
    {
        return blockSlotCapacity != null && occupiedBlockSlots != null
                && occupiedBlockSlots < blockSlotCapacity;
    }

    private static Integer nonNegative(Integer value)
    {
        return value == null ? null : Math.max(0, value);
    }
}
