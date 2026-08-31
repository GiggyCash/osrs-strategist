package compass;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A temporary soft recommendation adjustment.
 *
 * <p>This is intentionally different from a cooldown. A cooldown hides an
 * activity completely; a timed adjustment merely nudges ranking. Milestone
 * completion uses this to encourage variety without preventing Compass from
 * recommending the same skill again when it is still clearly the best move.</p>
 */
@Getter
@RequiredArgsConstructor
public final class TimedScoreAdjustment
{
    private final double scoreDelta;
    private final long expiresAtMillis;




    public boolean isExpired(long nowMillis)
    {
        return expiresAtMillis <= nowMillis;
    }
}
