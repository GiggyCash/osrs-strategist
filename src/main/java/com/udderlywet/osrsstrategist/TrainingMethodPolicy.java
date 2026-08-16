package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/** Account-mode and play-style guardrails for concrete training methods. */
@Singleton
public class TrainingMethodPolicy
{
    public boolean isAllowed(
            StrategyDataBundle data,
            TrainingMethod method,
            TrainingMethodMetadata metadata,
            boolean allowWildernessMethods)
    {
        if (method == null || metadata == null) return false;
        AccountSnapshot account = data == null ? null : data.getAccount();
        AccountMode mode = account == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(account.getAccountTypeCode());
        MembershipStatus membership = account == null
                ? MembershipStatus.UNKNOWN
                : account.getMembershipStatus();

        if (membership == MembershipStatus.FREE_TO_PLAY
                && !metadata.isFreeToPlayAllowed())
        {
            return false;
        }

        if (method.isWilderness() && !allowWildernessMethods)
        {
            return false;
        }

        if (mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
        {
            // Hardcore defaults are intentionally conservative. A future
            // explicit dangerous-content override can be added separately.
            if (method.isWilderness()
                    || !metadata.isHardcoreSafe()
                    || metadata.getRiskLevel() == RiskLevel.HIGH
                    || metadata.getRiskLevel() == RiskLevel.IRREVERSIBLE)
            {
                return false;
            }
        }

        if (mode == AccountMode.ULTIMATE_IRONMAN
                && !metadata.isUimFriendly())
        {
            return false;
        }

        if (AccountModePolicy.isRiskSensitive(mode)
                && metadata.getRiskLevel() == RiskLevel.IRREVERSIBLE)
        {
            return false;
        }

        return true;
    }

    public double scoreAdjustment(
            StrategyDataBundle data,
            TrainingMethodMetadata metadata,
            StrategyMode strategyMode,
            SessionIntent sessionIntent)
    {
        if (metadata == null) return 0.0;
        AccountSnapshot account = data == null ? null : data.getAccount();
        AccountMode mode = account == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(account.getAccountTypeCode());
        double score = intensityAdjustment(metadata.getIntensity(), strategyMode, sessionIntent);

        if (mode != null && mode.isIronLike())
        {
            if (metadata.isSelfSourceFriendly()) score += 3.0;
            switch (metadata.getCostTier())
            {
                case VERY_HIGH: score -= 12.0; break;
                case HIGH: score -= 8.0; break;
                case MODERATE: score -= 3.0; break;
                case PROFITABLE: score += 4.0; break;
                default: break;
            }
        }

        if (mode == AccountMode.ULTIMATE_IRONMAN && metadata.isUimFriendly())
        {
            score += 4.0;
        }

        if ((mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                && metadata.isHardcoreSafe())
        {
            score += 4.0;
        }

        if (AccountModePolicy.isRiskSensitive(mode))
        {
            if (metadata.getRiskLevel() == RiskLevel.MEDIUM) score -= 5.0;
            if (metadata.getRiskLevel() == RiskLevel.HIGH) score -= 10.0;
        }

        return score;
    }

    private static double intensityAdjustment(
            TrainingIntensity intensity,
            StrategyMode mode,
            SessionIntent intent)
    {
        double score = 0.0;
        StrategyMode safeMode = mode == null ? StrategyMode.BALANCED : mode;
        SessionIntent safeIntent = intent == null ? SessionIntent.PICK_FOR_ME : intent;

        switch (safeMode)
        {
            case EFFICIENT:
                if (intensity == TrainingIntensity.SWEATY) score += 5.0;
                if (intensity == TrainingIntensity.EFFICIENT) score += 7.0;
                if (intensity == TrainingIntensity.RELAXED) score -= 2.0;
                if (intensity == TrainingIntensity.AFK) score -= 3.0;
                break;
            case RELAXED:
                if (intensity == TrainingIntensity.AFK) score += 8.0;
                if (intensity == TrainingIntensity.RELAXED) score += 7.0;
                if (intensity == TrainingIntensity.BALANCED) score += 2.0;
                if (intensity == TrainingIntensity.SWEATY) score -= 9.0;
                break;
            case BALANCED:
            default:
                if (intensity == TrainingIntensity.BALANCED) score += 6.0;
                if (intensity == TrainingIntensity.EFFICIENT) score += 3.0;
                if (intensity == TrainingIntensity.RELAXED) score += 3.0;
                if (intensity == TrainingIntensity.SWEATY) score -= 1.0;
                break;
        }

        if (safeIntent == SessionIntent.AFK)
        {
            if (intensity == TrainingIntensity.AFK) score += 9.0;
            if (intensity == TrainingIntensity.RELAXED) score += 4.0;
            if (intensity == TrainingIntensity.SWEATY) score -= 12.0;
        }
        return score;
    }
}
