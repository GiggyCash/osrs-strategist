package com.udderlywet.osrsstrategist;

/**
 * How strongly Strategist should prefer freshness when several useful choices
 * are close together in score.
 *
 * <p>This setting never makes an unavailable or strategically bad activity
 * good. It only changes a small, capped family-level adjustment after normal
 * account-state, requirement, goal, risk, and preference scoring has happened.</p>
 */
public enum VarietyPreference
{
    /** Stay on a productive theme unless the player explicitly rejects it. */
    FOCUSED(0.35),

    /** Default balance between momentum and avoiding monotonous suggestions. */
    BALANCED(1.00),

    /** Prefer changing activity families when alternatives are similarly good. */
    FRESH(1.55);

    private final double multiplier;

    VarietyPreference(double multiplier)
    {
        this.multiplier = multiplier;
    }

    public double getMultiplier()
    {
        return multiplier;
    }
}
