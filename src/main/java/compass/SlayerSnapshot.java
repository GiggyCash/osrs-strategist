package compass;
import lombok.*;
import static java.lang.Math.*;

import java.util.*;


/** Current Slayer task and point-economy evidence observed from RuneLite. */
@Getter
public final class SlayerSnapshot
{
    final String taskName;
    final int remaining;
    final String masterName;
    final String taskLocation;
    final int points;
    final Integer taskStreak;
    final Integer questPoints;
    final Integer blockSlotCapacity;
    final Integer occupiedBlockSlots;
    final SlayerRewardSnapshot rewards;
    final List<SlayerTaskOffer> taskOffers;
    @Getter(AccessLevel.NONE)
    final Boolean mortimerIntroduced;
    final Confidence confidence;

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
        this.remaining = max(0, remaining);
        this.masterName = masterName;
        this.taskLocation = taskLocation;
        this.points = max(0, points);
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

    public SlayerState getAssignmentState()
    {
        if (hasTask()) return SlayerState.ASSIGNED;
        if (!taskOffers.isEmpty()) return SlayerState.CHOICE_PENDING;
        if (confidence == Confidence.VERIFIED && remaining == 0)
            return SlayerState.NO_TASK;
        return SlayerState.UNKNOWN;
    }

    /** True only when both capacity and current per-master usage are known. */
    public boolean hasKnownFreeBlockSlot()
    {
        return blockSlotCapacity != null && occupiedBlockSlots != null
                && occupiedBlockSlots < blockSlotCapacity;
    }

    static Integer nonNegative(Integer value)
    {
        return value == null ? null : max(0, value);
    }
}
