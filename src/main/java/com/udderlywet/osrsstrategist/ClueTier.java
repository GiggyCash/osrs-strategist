package com.udderlywet.osrsstrategist;

import java.util.Locale;

/**
 * Treasure Trail tiers plus membership eligibility.
 *
 * <p>Membership filtering belongs on the clue tier itself so every Compass
 * surface (DO NEXT, opportunities, strategy signals, future reminders) uses the
 * same rule. This prevents a members-only clue left in a bank from being
 * presented as actionable while the character is currently F2P.</p>
 */
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

    /**
     * Only beginner Treasure Trails are actionable on a F2P planning profile.
     * Unknown tiers stay eligible so Compass can surface them as Needs Info
     * rather than silently pretending it knows the tier.
     */
    public boolean isAvailableFor(MembershipStatus membershipStatus)
    {
        if (membershipStatus != MembershipStatus.P2P)
        {
            return this == BEGINNER;
        }
        return true;
    }

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
