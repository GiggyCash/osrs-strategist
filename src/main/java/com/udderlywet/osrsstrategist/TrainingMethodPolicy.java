package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/** Account-mode, restricted-build, and play-style guardrails for training methods. */
@Singleton
public class TrainingMethodPolicy
{
    public boolean isAllowed(
            GameData data,
            TrainingMethod method,
            TrainingMethodMetadata metadata,
            boolean allowWildernessMethods)
    {
        if (method == null || metadata == null) return false;
        var account = data == null ? null : data.account();
        AccountMode mode = account == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(account.getAccountTypeCode());
        MembershipStatus membership = account == null
                ? MembershipStatus.UNKNOWN
                : account.getMembershipStatus();

        if (!AccountBuildPolicy.allowsMethod(account, method)) return false;

        // UNKNOWN membership is intentionally treated like F2P here. The route
        // can widen as soon as membership is verified, but it can never leak a
        // members-only method into an F2P account during a transient read.
        if (membership != MembershipStatus.P2P
                && !metadata.isFreeToPlayAllowed())
        {
            return false;
        }

        if (method.isWilderness() && !allowWildernessMethods) return false;

        if (mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
        {
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
            GameData data,
            TrainingMethodMetadata metadata,
            StrategyMode strategyMode,
            SessionIntent sessionIntent)
    {
        if (metadata == null) return 0.0;
        var account = data == null ? null : data.account();
        AccountMode mode = account == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(account.getAccountTypeCode());
        double score = intensityAdjustment(
                metadata.getIntensity(), strategyMode, sessionIntent);

        // Account-specific method value now comes from sourced strategy
        // profiles and live readiness in TrainingMethodSelector. Keep this
        // policy focused on play-style fit and real risk instead of layering
        // arbitrary Iron/UIM/method-cost bonuses over the knowledge model.
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
        var score = 0.0;
        var safeMode = mode == null ? StrategyMode.BALANCED : mode;
        var safeIntent = intent == null ? SessionIntent.PICK_FOR_ME : intent;

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
            if (intensity == TrainingIntensity.BALANCED) score -= 2.0;
            if (intensity == TrainingIntensity.EFFICIENT) score -= 5.0;
            if (intensity == TrainingIntensity.SWEATY) score -= 12.0;
        }
        return score;
    }
}
