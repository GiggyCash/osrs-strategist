package compass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** A measured XP rate, or an honest indication that evidence is insufficient. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class XpRateEstimate
{
    public enum State
    {
        CALCULATING,
        READY
    }

    private final State state;
    private final long xpPerHour;
    private final int timedIntervals;


    public static XpRateEstimate calculating(int timedIntervals)
    {
        return new XpRateEstimate(State.CALCULATING, 0L,
                Math.max(0, timedIntervals));
    }

    public static XpRateEstimate ready(long xpPerHour, int timedIntervals)
    {
        return new XpRateEstimate(State.READY, Math.max(1L, xpPerHour),
                Math.max(0, timedIntervals));
    }

    public boolean isReady() { return state == State.READY; }
}
