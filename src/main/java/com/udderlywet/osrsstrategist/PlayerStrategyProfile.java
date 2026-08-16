package com.udderlywet.osrsstrategist;

/** Per-character planning preferences that survive logout/restart. */
public final class PlayerStrategyProfile
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
        this.activeGoal = activeGoal == null ? GoalType.MAX : activeGoal;
        this.useGroupStorage = useGroupStorage;
        this.collectionistMode = collectionistMode;
        this.allowWildernessMethods = allowWildernessMethods;
    }

    public static PlayerStrategyProfile fromConfig(OsrsStrategistConfig config)
    {
        return new PlayerStrategyProfile(
                config.strategyMode(), config.sessionIntent(),
                config.questTolerance(), config.activeGoal(),
                config.useGroupStorage(), config.collectionistMode(),
                config.allowWildernessMethods()
        );
    }

    public StrategyMode getStrategyMode() { return strategyMode; }
    public SessionIntent getSessionIntent() { return sessionIntent; }
    public QuestTolerance getQuestTolerance() { return questTolerance; }
    public GoalType getActiveGoal() { return activeGoal; }
    public boolean isUseGroupStorage() { return useGroupStorage; }
    public boolean isCollectionistMode() { return collectionistMode; }
    public boolean isAllowWildernessMethods() { return allowWildernessMethods; }
}
