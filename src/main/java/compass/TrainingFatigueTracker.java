package compass;
import lombok.*;

import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Lightweight live-session fatigue model.
 *
 * <p>This is intentionally not a permanent dislike. Sustained training of one
 * skill can temporarily reduce that skill's ranking in Balanced/Relaxed modes,
 * while Efficient mode remains focused on account efficiency. Hitpoints is
 * ignored because it passively changes alongside combat and would otherwise
 * break the continuity signal for the combat skill the player actually chose.</p>
 */
@Singleton
public class TrainingFatigueTracker
{
    static final long CONTINUITY_GAP_MILLIS = 10L * 60L * 1000L;
    static final long BALANCED_THRESHOLD_MILLIS = 45L * 60L * 1000L;
    static final long RELAXED_THRESHOLD_MILLIS = 30L * 60L * 1000L;
    static final long PENALTY_DURATION_MILLIS = 35L * 60L * 1000L;

    private Skill activeSkill;
    private int lastXp = -1;
    private long startedAtMillis;
    private long lastGainAtMillis;
    private long gainedXp;

    /** Records a RuneLite stat snapshot and returns a fatigue signal if due. */
    public FatigueSignal record(
            Skill skill,
            int currentXp,
            StrategyMode strategyMode)
    {
        return record(skill, currentXp, strategyMode, System.currentTimeMillis());
    }

    FatigueSignal record(
            Skill skill,
            int currentXp,
            StrategyMode strategyMode,
            long now)
    {
        if (skill == null || skill == Skill.HITPOINTS || currentXp < 0)
        {
            return FatigueSignal.none();
        }

        if (activeSkill != skill
                || lastGainAtMillis <= 0L
                || now - lastGainAtMillis > CONTINUITY_GAP_MILLIS)
        {
            begin(skill, currentXp, now);
            return FatigueSignal.none();
        }

        if (lastXp >= 0 && currentXp > lastXp)
        {
            gainedXp += (long) currentXp - lastXp;
            lastGainAtMillis = now;
        }
        lastXp = currentXp;

        if (strategyMode == null || strategyMode == StrategyMode.EFFICIENT)
        {
            return FatigueSignal.none();
        }

        var duration = Math.max(0L, now - startedAtMillis);
        long threshold = strategyMode == StrategyMode.RELAXED
                ? RELAXED_THRESHOLD_MILLIS
                : BALANCED_THRESHOLD_MILLIS;
        if (duration < threshold || gainedXp <= 0L)
        {
            return FatigueSignal.none();
        }

        // Relaxed rotates sooner and more strongly. Balanced still allows a
        // strategically valuable continuation to overcome the soft penalty.
        var penalty = strategyMode == StrategyMode.RELAXED ? -14.0 : -8.0;
        return new FatigueSignal(
                "skill:" + skill.name().toLowerCase(),
                penalty,
                PENALTY_DURATION_MILLIS,
                duration,
                gainedXp);
    }

    public void clear()
    {
        activeSkill = null;
        lastXp = -1;
        startedAtMillis = 0L;
        lastGainAtMillis = 0L;
        gainedXp = 0L;
    }
    private void begin(Skill skill, int xp, long now)
    {
        activeSkill = skill;
        lastXp = xp;
        startedAtMillis = now;
        lastGainAtMillis = now;
        gainedXp = 0L;
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class FatigueSignal
    {
        private static final FatigueSignal NONE = new FatigueSignal(
                null, 0.0, 0L, 0L, 0L);

        final String activityId;
        private final double scoreDelta;
        private final long durationMillis;
        private final long activeDurationMillis;
        private final long gainedXp;
        public static FatigueSignal none() { return NONE; }
        public boolean isPresent() { return activityId != null; }
    }
}
