package com.udderlywet.osrsstrategist;

import java.util.Locale;

public enum ClueTier
{
    BEGINNER(0.0),
    EASY(1.0),
    MEDIUM(2.0),
    HARD(3.5),
    ELITE(5.0),
    MASTER(7.0),
    UNKNOWN(0.0);

    private final double priorityBonus;

    ClueTier(double priorityBonus)
    {
        this.priorityBonus = priorityBonus;
    }

    public double getPriorityBonus() { return priorityBonus; }

    public static ClueTier fromText(String value)
    {
        if (value == null) return UNKNOWN;
        String normalized = value.toLowerCase(Locale.ROOT);
        for (ClueTier tier : values())
            if (tier != UNKNOWN && normalized.contains(tier.name().toLowerCase(Locale.ROOT)))
                return tier;
        return UNKNOWN;
    }
}
