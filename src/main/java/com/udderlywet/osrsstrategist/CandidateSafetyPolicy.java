package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/** Final access/build boundary shared by every recommendation family. */
@Singleton
public class CandidateSafetyPolicy
{
    public boolean isAllowed(Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null || context == null || context.getData() == null
                || context.getData().getAccount() == null)
        {
            return recommendation != null;
        }

        AccountSnapshot account = context.getData().getAccount();
        if (AccountMode.fromTypeCode(account.getAccountTypeCode())
                    == AccountMode.ULTIMATE_IRONMAN
                && requiresNormalBank(recommendation)) return false;
        return isAllowed(recommendation.getSafetyEvidence(), account);
    }

    public boolean isAllowed(CandidateSafetyEvidence evidence,
            StrategyContext context)
    {
        if (evidence == null || context == null || context.getData() == null
                || context.getData().getAccount() == null) return false;
        return isAllowed(evidence, context.getData().getAccount());
    }

    private static boolean isAllowed(CandidateSafetyEvidence evidence,
            AccountSnapshot account)
    {

        // Unannotated content is never assumed F2P-safe. This is the final
        // protection against a new provider forgetting its early access filter.
        if (account.getMembershipStatus() != MembershipStatus.P2P
                && evidence.getAccess() != CandidateSafetyEvidence.Access.F2P_SAFE)
        {
            return false;
        }

        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        if ((mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                && (evidence.getBuildEffect()
                == CandidateSafetyEvidence.BuildEffect.POTENTIALLY_IRREVERSIBLE
                || evidence.getBuildEffect()
                == CandidateSafetyEvidence.BuildEffect.UNKNOWN))
        {
            // When the candidate cannot prove its risk/build effects, preserving
            // a Hardcore life takes precedence over provider score.
            return false;
        }

        RestrictedBuildSuggestion suggestion = AccountBuildPolicy.detect(account);
        if (suggestion.getConfidence() == RecommendationConfidence.VERIFIED
                && suggestion.getType() == RestrictedBuildType.STANDARD)
        {
            return true;
        }

        switch (evidence.getBuildEffect())
        {
            case HARMLESS:
            case VERIFIED_SAFE:
                return true;
            case SKILL_XP:
                return evidence.getAffectedSkill() != null
                        && AccountBuildPolicy.allowsSkill(account,
                        evidence.getAffectedSkill());
            case POTENTIALLY_IRREVERSIBLE:
            case UNKNOWN:
            default:
                // Ambiguous and verified restricted signatures both fail closed
                // unless the provider supplied a harmless or verified-safe proof.
                return false;
        }
    }

    private static boolean requiresNormalBank(Recommendation recommendation)
    {
        RecommendationGuidance guidance = recommendation.getGuidance();
        if (guidance == null) return false;
        String text = (safe(guidance.getAction()) + " "
                + safe(guidance.getSupplies()) + " "
                + safe(guidance.getLocation())).toLowerCase(
                        java.util.Locale.ROOT);
        return text.contains("open the bank")
                || text.contains("open your bank")
                || text.contains("bank at ")
                || text.contains("bank near ")
                || text.contains("banking ores")
                || text.contains("bank the ")
                || text.contains("bank and repeat")
                || text.contains("bank, repeat")
                || text.contains("bank or ")
                || text.contains("banking upstairs")
                || text.contains("bank upstairs")
                || text.contains("withdraw bars")
                || text.contains("banked metal")
                || text.contains("withdraw from the bank");
    }

    private static String safe(String value)
    {
        return value == null ? "" : value;
    }
}
