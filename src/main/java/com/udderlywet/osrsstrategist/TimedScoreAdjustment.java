package com.udderlywet.osrsstrategist;

/**
 * A temporary soft recommendation adjustment.
 *
 * <p>This is intentionally different from a cooldown. A cooldown hides an
 * activity completely; a timed adjustment merely nudges ranking. Milestone
 * completion uses this to encourage variety without preventing Strategist from
 * recommending the same skill again when it is still clearly the best move.</p>
 */
public final class TimedScoreAdjustment
{
    private final double scoreDelta;
    private final long expiresAtMillis;

    public TimedScoreAdjustment(
            double scoreDelta,
            long expiresAtMillis)
    {
        this.scoreDelta = scoreDelta;
        this.expiresAtMillis = expiresAtMillis;
    }

    public double getScoreDelta()
    {
        return scoreDelta;
    }

    public long getExpiresAtMillis()
    {
        return expiresAtMillis;
    }

    public boolean isExpired(long nowMillis)
    {
        return expiresAtMillis <= nowMillis;
    }
}
