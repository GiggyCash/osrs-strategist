package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Current Slayer task and point-economy evidence observed from RuneLite. */
public final class SlayerSnapshot
{
    @Getter
    private final String taskName;
    @Getter
    private final int remaining;
    @Getter
    private final String masterName;
    @Getter
    private final String taskLocation;
    @Getter
    private final int points;
    @Getter
    private final Integer taskStreak;
    @Getter
    private final Integer questPoints;
    @Getter
    private final Integer blockSlotCapacity;
    @Getter
    private final Integer occupiedBlockSlots;
    @Getter
    private final SlayerRewardSnapshot rewards;
    @Getter
    private final List<SlayerTaskOffer> taskOffers;
    private final Boolean mortimerIntroduced;
    @Getter
    private final Confidence confidence;

    /** Compatibility constructor retained for older callers. */
    public SlayerSnapshot(
            String taskName,
            int remaining,
            String masterName,
            int points,
            Confidence confidence)
    {
        this(taskName, remaining, masterName, null, points,
                null, null, null, null, null, null, null, confidence);
    }

    public SlayerSnapshot(
            String taskName,
            int remaining,
            String masterName,
            String taskLocation,
            int points,
            Confidence confidence)
    {
        this(taskName, remaining, masterName, taskLocation, points,
                null, null, null, null, null, null, null, confidence);
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
            Confidence confidence)
    {
        this(taskName, remaining, masterName, taskLocation, points, taskStreak,
                questPoints, blockSlotCapacity, occupiedBlockSlots, null,
                null, null,
                confidence);
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
            SlayerRewardSnapshot rewards,
            Confidence confidence)
    {
        this(taskName, remaining, masterName, taskLocation, points, taskStreak,
                questPoints, blockSlotCapacity, occupiedBlockSlots, rewards,
                null, null, confidence);
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
            SlayerRewardSnapshot rewards,
            List<SlayerTaskOffer> taskOffers,
            Boolean mortimerIntroduced,
            Confidence confidence)
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
        this.rewards = rewards == null ? SlayerRewardSnapshot.unknown() : rewards;
        this.taskOffers = Collections.unmodifiableList(taskOffers == null
                ? new ArrayList<>() : new ArrayList<>(taskOffers));
        this.mortimerIntroduced = mortimerIntroduced;
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED
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
                null,
                null,
                null,
                Confidence.CHECK_NEEDED
        );
    }

    public Boolean isMortimerIntroduced() { return mortimerIntroduced; }

    public boolean hasTask()
    {
        return taskName != null && !taskName.trim().isEmpty() && remaining > 0;
    }

    public SlayerAssignmentState getAssignmentState()
    {
        if (hasTask()) return SlayerAssignmentState.ASSIGNED;
        if (!taskOffers.isEmpty()) return SlayerAssignmentState.CHOICE_PENDING;
        if (confidence == Confidence.VERIFIED && remaining == 0)
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
