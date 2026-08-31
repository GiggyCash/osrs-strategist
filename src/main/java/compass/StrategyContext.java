package compass;

import lombok.Getter;
import lombok.experimental.Accessors;

/** Immutable intent/context passed to strategy modules. */
@Getter
@Accessors(fluent = true)
public final class StrategyContext extends PlayerStrategyProfile
{
    private final GameData data;
    private final PreferenceProfile preferenceProfile;
    private final AccountMode accountMode;

    public StrategyContext(
            GameData data,
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
            GameData data,
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
        this.accountMode = data == null || data.account() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(data.account().modeCode());
    }

}
