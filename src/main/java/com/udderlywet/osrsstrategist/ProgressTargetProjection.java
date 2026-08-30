package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Remaining work and ETA for the current skill target. */
public final class ProgressTargetProjection
{
    public enum State
    {
        NO_TARGET,
        CALCULATING,
        READY,
        COMPLETE
    }

    @Getter
    private final State state;
    @Getter
    private final ProgressTarget target;
    @Getter
    private final int xpRemaining;
    @Getter
    private final long etaMillis;

    private ProgressTargetProjection(
            State state,
            ProgressTarget target,
            int xpRemaining,
            long etaMillis)
    {
        this.state = state;
        this.target = target;
        this.xpRemaining = Math.max(0, xpRemaining);
        this.etaMillis = Math.max(0L, etaMillis);
    }

    public static ProgressTargetProjection noTarget()
    {
        return new ProgressTargetProjection(State.NO_TARGET, null, 0, 0L);
    }

    public static ProgressTargetProjection calculating(
            ProgressTarget target, int xpRemaining)
    {
        return new ProgressTargetProjection(State.CALCULATING, target,
                xpRemaining, 0L);
    }

    public static ProgressTargetProjection ready(
            ProgressTarget target, int xpRemaining, long etaMillis)
    {
        return new ProgressTargetProjection(State.READY, target,
                xpRemaining, etaMillis);
    }

    public static ProgressTargetProjection complete(ProgressTarget target)
    {
        return new ProgressTargetProjection(State.COMPLETE, target, 0, 0L);
    }

}
