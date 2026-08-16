package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A deliberately slow-moving preference model. Long-term preference and
 * temporary recommendation cooldowns are separate so "Not Today" does not
 * teach Strategist that the player permanently dislikes an activity.
 */
public final class PreferenceProfile
{
    private static final long ONE_HOUR_MILLIS = 60L * 60L * 1000L;
    private static final long SIX_HOURS_MILLIS = 6L * ONE_HOUR_MILLIS;
    private static final long ONE_DAY_MILLIS = 24L * ONE_HOUR_MILLIS;

    private final Map<String, Double> weights = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    public double weightFor(String activityId)
    {
        return weights.getOrDefault(activityId, 0.0);
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

    public void apply(String activityId, FeedbackAction action)
    {
        long now = System.currentTimeMillis();
        double delta = 0.0;

        switch (action)
        {
            case DO_THIS:
                delta = 0.20;
                cooldowns.remove(activityId);
                break;

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

    public void clear()
    {
        weights.clear();
        cooldowns.clear();
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
}
