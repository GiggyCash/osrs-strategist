package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A deliberately slow-moving preference model. Skipping an activity once does
 * not erase it. Repeated feedback gradually changes its weight, while important
 * progression can still override a dislike when the payoff is large enough.
 */
public final class PreferenceProfile
{
    private final Map<String, Double> weights = new HashMap<>();

    public double weightFor(String activityId)
    {
        return weights.getOrDefault(activityId, 0.0);
    }

    public void apply(String activityId, FeedbackAction action)
    {
        double delta;
        switch (action)
        {
            case DO_THIS:
                delta = 0.20;
                break;

            case LATER:
                delta = -0.05;
                break;

            case NOT_TODAY:
                delta = -0.10;
                break;

            case DISLIKE:
                delta = -0.25;
                break;

            default:
                delta = 0.0;
                break;
        }

        double next = Math.max(
                -1.5,
                Math.min(
                        1.5,
                        weightFor(activityId) + delta
                )
        );

        weights.put(activityId, next);
    }

    public void clear()
    {
        weights.clear();
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

    public Map<String, Double> snapshot()
    {
        return Collections.unmodifiableMap(
                new HashMap<>(weights)
        );
    }
}
