package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Immutable intent/context passed to strategy modules. */
@Getter
public final class StrategyContext extends PlayerStrategyProfile
{
    private final StrategyDataBundle data;
    private final PreferenceProfile preferenceProfile;
    private final AccountMode accountMode;

    public StrategyContext(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            PreferenceProfile preferenceProfile)
    {
        this(data, strategyMode, sessionIntent, questTolerance, activeGoal,
                useGroupStorage, collectionistMode, false, preferenceProfile);
    }

    public StrategyContext(
            StrategyDataBundle data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile)
    {
        super(strategyMode, sessionIntent, questTolerance, activeGoal,
                useGroupStorage, collectionistMode, allowWildernessMethods);
        this.data = data;
        this.preferenceProfile = preferenceProfile == null ? new PreferenceProfile() : preferenceProfile;
        this.accountMode = data == null || data.getAccount() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(data.getAccount().getAccountTypeCode());
    }

}
