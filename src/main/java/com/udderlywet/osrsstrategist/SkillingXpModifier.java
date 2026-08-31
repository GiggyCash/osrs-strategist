package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** XP multiplier Compass can safely plan around because the required set is observed. */
@Getter
public final class SkillingXpModifier
{
    private final double multiplier;
    private final String label;

    public SkillingXpModifier(double multiplier, String label)
    {
        this.multiplier = multiplier <= 0 ? 1.0 : multiplier;
        this.label = label;
    }


    public static SkillingXpModifier none()
    {
        return new SkillingXpModifier(1.0, null);
    }
}
