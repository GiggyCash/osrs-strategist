package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Per-character planning preferences that survive logout/restart. */
@Getter
public class PlayerStrategyProfile
{
    private final StrategyMode strategyMode;
    private final SessionIntent sessionIntent;
    private final QuestTolerance questTolerance;
    private final GoalType activeGoal;
    private final boolean useGroupStorage;
    private final boolean collectionistMode;
    private final boolean allowWildernessMethods;

    public PlayerStrategyProfile(
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode)
    {
        this(strategyMode, sessionIntent, questTolerance, activeGoal,
                useGroupStorage, collectionistMode, false);
    }

    public PlayerStrategyProfile(
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            boolean allowWildernessMethods)
    {
        this.strategyMode = strategyMode == null ? StrategyMode.BALANCED : strategyMode;
        this.sessionIntent = sessionIntent == null ? SessionIntent.PICK_FOR_ME : sessionIntent;
        this.questTolerance = questTolerance == null ? QuestTolerance.NORMAL : questTolerance;
        this.activeGoal = activeGoal == null ? GoalType.AUTOMATIC : activeGoal;
        this.useGroupStorage = useGroupStorage;
        this.collectionistMode = collectionistMode;
        this.allowWildernessMethods = allowWildernessMethods;
    }

    public static PlayerStrategyProfile fromConfig(OsrsStrategistConfig config)
    {
        var configuredGoal = config.activeGoal();
        return new PlayerStrategyProfile(
                config.strategyMode(), config.sessionIntent(),
                config.questTolerance(), configuredGoal == null
                        ? GoalType.AUTOMATIC
                        : configuredGoal.toPlanningGoal(),
                config.useGroupStorage(), config.collectionistMode(),
                config.allowWildernessMethods()
        );
    }


    PlayerStrategyProfile sanitizedForPublicProduct()
    {
        if (PlayerGoal.isPlayerFacing(activeGoal)) return this;
        return new PlayerStrategyProfile(strategyMode, sessionIntent,
                questTolerance, GoalType.AUTOMATIC, useGroupStorage,
                collectionistMode, allowWildernessMethods);
    }
}
