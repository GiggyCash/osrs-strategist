package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/** Final access/build boundary shared by every recommendation family. */
@Singleton
public class CandidateSafetyPolicy
{
    public boolean isAllowed(Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null || context == null || context.data() == null
                || context.data().account() == null)
        {
            return recommendation != null;
        }

        var account = context.data().account();
        if (AccountMode.fromTypeCode(account.getAccountTypeCode())
                    == AccountMode.ULTIMATE_IRONMAN
                && (recommendation.getSafetyEvidence()
                        .isConventionalBankRequired()
                    || recommendation.getSafetyEvidence()
                        .hasUnverifiedDangerousStorage()
                    || recommendation.getGuidance() != null
                        && recommendation.getGuidance().getBankingBehavior()
                            == MethodBankingBehavior.CONVENTIONAL_BANK_LOOP))
            return false;
        if (recommendation.getSafetyEvidence().hasInvalidCurrentExecution())
            return false;
        return isAllowed(recommendation.getSafetyEvidence(), account);
    }

    public boolean isAllowed(SafetyEvidence evidence,
            StrategyContext context)
    {
        if (evidence == null || context == null || context.data() == null
                || context.data().account() == null) return false;
        return isAllowed(evidence, context.data().account());
    }

    private static boolean isAllowed(SafetyEvidence evidence,
            AccountSnapshot account)
    {

        if (evidence.hasInvalidCurrentExecution()) return false;

        // Unannotated content is never assumed F2P-safe. This is the final
        // protection against a new provider forgetting its early access filter.
        if (account.getMembershipStatus() != MembershipStatus.P2P
                && evidence.getAccess() != SafetyEvidence.Access.F2P_SAFE)
        {
            return false;
        }

        var mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        if ((mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                && (evidence.getBuildEffect()
                == SafetyEvidence.BuildEffect.POTENTIALLY_IRREVERSIBLE
                || evidence.getBuildEffect()
                == SafetyEvidence.BuildEffect.UNKNOWN))
        {
            // When the candidate cannot prove its risk/build effects, preserving
            // a Hardcore life takes precedence over provider score.
            return false;
        }

        var suggestion = AccountBuildPolicy.detect(account);
        if (suggestion.getConfidence() == Confidence.VERIFIED
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

}
