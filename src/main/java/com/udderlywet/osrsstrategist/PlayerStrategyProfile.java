package com.udderlywet.osrsstrategist;

/**
 * Per-character planning preferences that should survive logout/restart.
 *
 * <p>Learned activity weights are stored separately in PreferenceProfile.
 * This class holds the player's explicit planning choices such as strategy
 * style and big goal.</p>
 */
public final class PlayerStrategyProfile
{
    private final StrategyMode strategyMode;
    private final SessionIntent sessionIntent;
    private final QuestTolerance questTolerance;
    private final GoalType activeGoal;
    private final boolean useGroupStorage;
    private final boolean collectionistMode;

    public PlayerStrategyProfile(
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode)
    {
        this.strategyMode = strategyMode == null
                ? StrategyMode.BALANCED
                : strategyMode;
        this.sessionIntent = sessionIntent == null
                ? SessionIntent.PICK_FOR_ME
                : sessionIntent;
        this.questTolerance = questTolerance == null
                ? QuestTolerance.NORMAL
                : questTolerance;
        this.activeGoal = activeGoal == null
                ? GoalType.MAX
                : activeGoal;
        this.useGroupStorage = useGroupStorage;
        this.collectionistMode = collectionistMode;
    }

    public static PlayerStrategyProfile fromConfig(
            OsrsStrategistConfig config)
    {
        return new PlayerStrategyProfile(
                config.strategyMode(),
                config.sessionIntent(),
                config.questTolerance(),
                config.activeGoal(),
                config.useGroupStorage(),
                config.collectionistMode()
        );
    }

    public StrategyMode getStrategyMode() { return strategyMode; }
    public SessionIntent getSessionIntent() { return sessionIntent; }
    public QuestTolerance getQuestTolerance() { return questTolerance; }
    public GoalType getActiveGoal() { return activeGoal; }
    public boolean isUseGroupStorage() { return useGroupStorage; }
    public boolean isCollectionistMode() { return collectionistMode; }
}
