package com.udderlywet.osrsstrategist;

/**
 * Small helper that encodes Strategist's most important safety rule:
 * unknown data is never silently promoted to verified data.
 */
public final class NoGuessingPolicy
{
    private NoGuessingPolicy()
    {
    }

    public static RecommendationConfidence fromCapability(
            CapabilityState state)
    {
        if (state == CapabilityState.VERIFIED)
        {
            return RecommendationConfidence.VERIFIED;
        }

        if (state == CapabilityState.BLOCKED)
        {
            return RecommendationConfidence.BLOCKED;
        }

        return RecommendationConfidence.CHECK_NEEDED;
    }

    public static RecommendationConfidence combine(
            RecommendationConfidence first,
            RecommendationConfidence second)
    {
        if (first == RecommendationConfidence.BLOCKED
                || second == RecommendationConfidence.BLOCKED)
        {
            return RecommendationConfidence.BLOCKED;
        }

        if (first == RecommendationConfidence.CHECK_NEEDED
                || second == RecommendationConfidence.CHECK_NEEDED)
        {
            return RecommendationConfidence.CHECK_NEEDED;
        }

        return RecommendationConfidence.VERIFIED;
    }
}
