package com.udderlywet.osrsstrategist;

/**
 * Shared safety policy for irreversible/high-cost strategy suggestions.
 */
public final class RiskPolicy
{
    private RiskPolicy()
    {
    }

    public static boolean mustWarn(
            AccountMode mode,
            RiskLevel level)
    {
        if (level == null || level == RiskLevel.NONE)
        {
            return false;
        }

        if (level == RiskLevel.HIGH
                || level == RiskLevel.IRREVERSIBLE)
        {
            return true;
        }

        return AccountModePolicy.isRiskSensitive(mode)
                && level.ordinal() >= RiskLevel.MEDIUM.ordinal();
    }

    public static boolean mustRequireExplicitConfirmation(
            RiskLevel level)
    {
        return level == RiskLevel.IRREVERSIBLE;
    }
}
