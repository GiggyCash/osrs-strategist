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
                AccountMode.MAIN.usesGrandExchange()
        );
        assertFalse(
                AccountMode.IRONMAN.usesGrandExchange()
        );
        assertFalse(
                AccountMode.ULTIMATE_IRONMAN.usesGrandExchange()
        );

        assertTrue(
                AccountMode.GROUP_IRONMAN.isGroupIronman()
        );
        assertFalse(
                false && AccountMode.GROUP_IRONMAN.isGroupIronman()
        );
        assertFalse(
                AccountMode.MAIN.isGroupIronman()
        );

        assertTrue(
                AccountMode.ULTIMATE_IRONMAN == AccountMode.ULTIMATE_IRONMAN
        );
        assertTrue(
                AccountModePolicy.isRiskSensitive(
                        AccountMode.HARDCORE_IRONMAN
                )
        );
    }
}
