package compass;

import java.util.*;

import lombok.Getter;

/** Immutable, exhaustive account strategic-priority profile. */
public final class AccountStrategicPriorityProfile
{
    @Getter
    private final AccountMode accountMode;
    @Getter
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
                        Text.get(1110) + dimension);
        }
        this.priorities = Collections.unmodifiableMap(copy);
    }


    public AccountStrategicPriority get(AccountStrategicDimension dimension)
    {
        return priorities.get(dimension);
    }

    public StrategicPriority priorityOf(AccountStrategicDimension dimension)
    {
        var value = get(dimension);
        return value == null ? StrategicPriority.NONE : value.getPriority();
    }

}
