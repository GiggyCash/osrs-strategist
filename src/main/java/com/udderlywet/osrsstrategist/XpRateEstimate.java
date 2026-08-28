package com.udderlywet.osrsstrategist;

/** A measured XP rate, or an honest indication that evidence is insufficient. */
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

    private XpRateEstimate(State state, long xpPerHour, int timedIntervals)
    {
        this.state = state;
        this.xpPerHour = xpPerHour;
        this.timedIntervals = timedIntervals;
    }

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

    public State getState() { return state; }
    public boolean isReady() { return state == State.READY; }
    public long getXpPerHour() { return xpPerHour; }
    public int getTimedIntervals() { return timedIntervals; }
}
