package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable, exhaustive account strategic-priority profile. */
public final class AccountStrategicPriorityProfile
{
    private final AccountMode accountMode;
    private final Map<AccountStrategicDimension, AccountStrategicPriority>
            priorities;

    AccountStrategicPriorityProfile(AccountMode accountMode,
            Map<AccountStrategicDimension, AccountStrategicPriority> values)
    {
        this.accountMode = accountMode == null ? AccountMode.UNKNOWN : accountMode;
        EnumMap<AccountStrategicDimension, AccountStrategicPriority> copy =
                new EnumMap<>(AccountStrategicDimension.class);
        if (values != null) copy.putAll(values);
        for (AccountStrategicDimension dimension
                : AccountStrategicDimension.values())
        {
            if (!copy.containsKey(dimension))
                throw new IllegalArgumentException(
                        "Missing account priority " + dimension);
        }
        this.priorities = Collections.unmodifiableMap(copy);
    }

    public AccountMode getAccountMode() { return accountMode; }

    public AccountStrategicPriority get(AccountStrategicDimension dimension)
    {
        return priorities.get(dimension);
    }

    public StrategicPriority priorityOf(AccountStrategicDimension dimension)
    {
        AccountStrategicPriority value = get(dimension);
        return value == null ? StrategicPriority.NONE : value.getPriority();
    }

    public Map<AccountStrategicDimension, AccountStrategicPriority> getPriorities()
    {
        return priorities;
    }
}
