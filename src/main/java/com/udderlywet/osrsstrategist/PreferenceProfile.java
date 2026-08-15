package com.udderlywet.osrsstrategist;

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
            case DO_THIS: delta = 0.20; break;
            case LATER: delta = -0.05; break;
            case NOT_TODAY: delta = -0.10; break;
            case DISLIKE: delta = -0.25; break;
            default: delta = 0.0;
        }
        double next = Math.max(-1.5, Math.min(1.5, weightFor(activityId) + delta));
        weights.put(activityId, next);
    }
}
