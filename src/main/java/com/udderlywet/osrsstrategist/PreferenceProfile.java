package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A deliberately slow-moving preference model.
 *
 * <p>Three concepts stay separate:</p>
 * <ul>
 *     <li>Long-term preference: what the player generally likes/dislikes.</li>
 *     <li>Cooldowns: activities temporarily hidden by explicit feedback.</li>
 *     <li>Timed score adjustments: soft nudges such as post-milestone variety.</li>
 * </ul>
 *
 * <p>Keeping them separate prevents "Not Today" or a recently completed skill
 * from accidentally becoming a permanent dislike.</p>
 */
public final class PreferenceProfile
{
    private static final String SEMANTIC_PREFIX = "semantic:";
    private static final long ONE_HOUR_MILLIS = 60L * 60L * 1000L;
    private static final long SIX_HOURS_MILLIS = 6L * ONE_HOUR_MILLIS;
    private static final long ONE_DAY_MILLIS = 24L * ONE_HOUR_MILLIS;

    private final Map<String, Double> weights = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, TimedScoreAdjustment> timedAdjustments =
            new HashMap<>();

    public double weightFor(String activityId)
    {
        return weights.getOrDefault(activityId, 0.0);
    }

    public double timedScoreAdjustmentFor(String activityId)
    {
        TimedScoreAdjustment adjustment = timedAdjustments.get(activityId);

        if (adjustment == null)
        {
            return 0.0;
        }

        long now = System.currentTimeMillis();
        if (adjustment.isExpired(now))
        {
            timedAdjustments.remove(activityId);
            return 0.0;
        }

        return adjustment.getScoreDelta();
    }

    public boolean isOnCooldown(String activityId)
    {
        Long cooldownUntil = cooldowns.get(activityId);

        if (cooldownUntil == null)
        {
            return false;
        }

        if (cooldownUntil <= System.currentTimeMillis())
        {
            cooldowns.remove(activityId);
            return false;
        }

        return true;
    }

    /**
     * Adds a soft temporary ranking adjustment rather than hiding an activity.
     * This is ideal for milestone completion: we encourage a fresh suggestion,
     * but a strategically important continuation can still win the ranking.
     */
    public void addTemporaryScoreAdjustment(
            String activityId,
            double scoreDelta,
            long durationMillis)
    {
        if (activityId == null || durationMillis <= 0L)
        {
            return;
        }

        timedAdjustments.put(
                activityId,
                new TimedScoreAdjustment(
                        scoreDelta,
                        System.currentTimeMillis() + durationMillis
                )
        );
    }

    public void apply(String activityId, FeedbackAction action)
    {
        long now = System.currentTimeMillis();
        double delta = 0.0;

        switch (action)
        {
            case LATER:
                // "Later" is temporary, not a dislike.
                cooldowns.put(
                        activityId,
                        now + ONE_HOUR_MILLIS
                );
                break;

            case NOT_TODAY:
                // Hide it for a full day without damaging its long-term score.
                cooldowns.put(
                        activityId,
                        now + ONE_DAY_MILLIS
                );
                break;

            case DISLIKE:
                // A dislike should affect future ranking, but important
                // progression can still overcome the negative weight later.
                delta = -0.25;
                cooldowns.put(
                        activityId,
                        now + SIX_HOURS_MILLIS
                );
                break;

            default:
                break;
        }

        if (delta != 0.0)
        {
            double next = Math.max(
                    -1.5,
                    Math.min(
                            1.5,
                            weightFor(activityId) + delta
                    )
            );

            weights.put(activityId, next);
        }
    }

    /** Apply feedback to a deduplicated action so provider aliases cannot rebound. */
    public void applySemantic(String semanticKey, FeedbackAction action)
    {
        if (semanticKey == null || semanticKey.trim().isEmpty()
                || action == null) return;
        apply(SEMANTIC_PREFIX + semanticKey, action);
    }

    public boolean isSemanticOnCooldown(String semanticKey)
    {
        return semanticKey != null
                && isOnCooldown(SEMANTIC_PREFIX + semanticKey);
    }

    public double semanticWeightFor(String semanticKey)
    {
        return semanticKey == null ? 0.0
                : weightFor(SEMANTIC_PREFIX + semanticKey);
    }

    public double semanticTimedScoreAdjustmentFor(String semanticKey)
    {
        return semanticKey == null ? 0.0
                : timedScoreAdjustmentFor(SEMANTIC_PREFIX + semanticKey);
    }

    public void clear()
    {
        weights.clear();
        cooldowns.clear();
        timedAdjustments.clear();
    }

    public void replaceAll(Map<String, Double> storedWeights)
    {
        weights.clear();

        if (storedWeights == null)
        {
            return;
        }

        for (Map.Entry<String, Double> entry : storedWeights.entrySet())
        {
            String activityId = entry.getKey();
            Double weight = entry.getValue();

            if (activityId == null || weight == null)
            {
                continue;
            }

            double clamped = Math.max(
                    -1.5,
                    Math.min(1.5, weight)
            );

            weights.put(activityId, clamped);
        }
    }

    public void replaceCooldowns(Map<String, Long> storedCooldowns)
    {
        cooldowns.clear();

        if (storedCooldowns == null)
        {
            return;
        }

        long now = System.currentTimeMillis();

        for (Map.Entry<String, Long> entry : storedCooldowns.entrySet())
        {
            String activityId = entry.getKey();
            Long cooldownUntil = entry.getValue();

            if (activityId == null
                    || cooldownUntil == null
                    || cooldownUntil <= now)
            {
                continue;
            }

            cooldowns.put(
                    activityId,
                    cooldownUntil
            );
        }
    }

    public void replaceTimedAdjustments(
            Map<String, TimedScoreAdjustment> storedAdjustments)
    {
        timedAdjustments.clear();

        if (storedAdjustments == null)
        {
            return;
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<String, TimedScoreAdjustment> entry
                : storedAdjustments.entrySet())
        {
            String activityId = entry.getKey();
            TimedScoreAdjustment adjustment = entry.getValue();

            if (activityId == null
                    || adjustment == null
                    || adjustment.isExpired(now))
            {
                continue;
            }

            timedAdjustments.put(activityId, adjustment);
        }
    }

    public Map<String, Double> snapshot()
    {
        return Collections.unmodifiableMap(
                new HashMap<>(weights)
        );
    }

    public Map<String, Long> cooldownSnapshot()
    {
        return Collections.unmodifiableMap(
                new HashMap<>(cooldowns)
        );
    }

    public Map<String, TimedScoreAdjustment> timedAdjustmentSnapshot()
    {
        // Purge expired entries before serializing the profile.
        for (String activityId : new HashMap<>(timedAdjustments).keySet())
        {
            timedScoreAdjustmentFor(activityId);
        }

        return Collections.unmodifiableMap(
                new HashMap<>(timedAdjustments)
        );
    }
}
