package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for item protection and irreversible-action warnings.
 */
public class SafetyFoundationTest
{
    @Test
    public void protectedItemProfileHonorsExplicitPlayerChoice()
    {
        ProtectedItemProfile profile = new ProtectedItemProfile();

        profile.protect(4151);
        assertTrue(profile.isProtected(4151));

        profile.unprotect(4151);
        assertFalse(profile.isProtected(4151));
    }

    @Test
    public void irreversibleActionsAlwaysWarnAndRequireConfirmation()
    {
        assertTrue(
                RiskPolicy.mustWarn(
                        AccountMode.MAIN,
                        RiskLevel.IRREVERSIBLE
                )
        );
        assertTrue(
                RiskPolicy.mustRequireExplicitConfirmation(
                        RiskLevel.IRREVERSIBLE
                )
        );
    }

    @Test
    public void riskSensitiveAccountsWarnAtMediumRisk()
    {
        assertTrue(
                RiskPolicy.mustWarn(
                        AccountMode.ULTIMATE_IRONMAN,
                        RiskLevel.MEDIUM
                )
        );
        assertFalse(
                RiskPolicy.mustWarn(
                        AccountMode.MAIN,
                        RiskLevel.MEDIUM
                )
        );
    }
}
