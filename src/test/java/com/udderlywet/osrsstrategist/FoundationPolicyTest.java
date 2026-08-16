package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guardrail tests for rules that must remain true as Strategist grows.
 */
public class FoundationPolicyTest
{
    @Test
    public void noGuessingPreservesUnknownAndBlockedStates()
    {
        assertEquals(
                RecommendationConfidence.VERIFIED,
                NoGuessingPolicy.fromCapability(
                        CapabilityState.VERIFIED
                )
        );
        assertEquals(
                RecommendationConfidence.CHECK_NEEDED,
                NoGuessingPolicy.fromCapability(
                        CapabilityState.UNKNOWN
                )
        );
        assertEquals(
                RecommendationConfidence.BLOCKED,
                NoGuessingPolicy.fromCapability(
                        CapabilityState.BLOCKED
                )
        );
    }

    @Test
    public void blockedConfidenceDominatesCombinedResult()
    {
        assertEquals(
                RecommendationConfidence.BLOCKED,
                NoGuessingPolicy.combine(
                        RecommendationConfidence.VERIFIED,
                        RecommendationConfidence.BLOCKED
                )
        );
        assertEquals(
                RecommendationConfidence.CHECK_NEEDED,
                NoGuessingPolicy.combine(
                        RecommendationConfidence.VERIFIED,
                        RecommendationConfidence.CHECK_NEEDED
                )
        );
    }

    @Test
    public void accountModeRulesKeepRestrictedAccountsRestricted()
    {
        assertTrue(
                AccountModePolicy.mayUseGrandExchange(
                        AccountMode.MAIN
                )
        );
        assertFalse(
                AccountModePolicy.mayUseGrandExchange(
                        AccountMode.IRONMAN
                )
        );
        assertFalse(
                AccountModePolicy.mayUseGrandExchange(
                        AccountMode.ULTIMATE_IRONMAN
                )
        );

        assertTrue(
                AccountModePolicy.mayUseGroupStorage(
                        AccountMode.GROUP_IRONMAN,
                        true
                )
        );
        assertFalse(
                AccountModePolicy.mayUseGroupStorage(
                        AccountMode.GROUP_IRONMAN,
                        false
                )
        );
        assertFalse(
                AccountModePolicy.mayUseGroupStorage(
                        AccountMode.MAIN,
                        true
                )
        );

        assertTrue(
                AccountModePolicy.requiresCapabilityCheckedStorage(
                        AccountMode.ULTIMATE_IRONMAN
                )
        );
        assertTrue(
                AccountModePolicy.isRiskSensitive(
                        AccountMode.HARDCORE_IRONMAN
                )
        );
    }
}
