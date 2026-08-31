package compass;

/**
 * Central account-mode rules used by planners before they recommend a route.
 *
 * <p>Keeping these rules in one place prevents accidental GE suggestions on
 * irons, inappropriate group-storage assumptions, or UIM storage advice that
 * ignores the account's restrictions.</p>
 */
public final class AccountModePolicy
{
    private AccountModePolicy()
    {
    }

    public static boolean mayUseGrandExchange(AccountMode mode)
    {
        return mode == AccountMode.MAIN;
    }

    public static boolean mayUseGroupStorage(
            AccountMode mode,
            boolean userEnabled)
    {
        return userEnabled
                && mode != null
                && mode.isGroupIronman();
    }

    public static boolean requiresSelfSourcing(AccountMode mode)
    {
        return mode != null && mode.isIronLike();
    }

    public static boolean requiresCapabilityCheckedStorage(AccountMode mode)
    {
        return mode == AccountMode.ULTIMATE_IRONMAN;
    }

    public static boolean isRiskSensitive(AccountMode mode)
    {
        return mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN
                || mode == AccountMode.ULTIMATE_IRONMAN;
    }
}
