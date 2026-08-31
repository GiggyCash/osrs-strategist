package compass;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guardrail tests for rules that must remain true as Compass grows.
 */
public class FoundationPolicyTest
{
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
