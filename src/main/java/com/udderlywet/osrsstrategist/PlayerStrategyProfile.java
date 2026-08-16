package com.udderlywet.osrsstrategist;

/**
 * Per-character planning preferences that survive logout/restart.
 *
 * <p>Getters are deliberately null-safe because Gson may load a profile written
 * by an older Strategist version before a newer field existed. This lets us add
 * customization without breaking long-lived RuneLite character profiles.</p>
 */
public final class PlayerStrategyProfile
{
    private final StrategyMode strategyMode;
    private final SessionIntent sessionIntent;
    private final QuestTolerance questTolerance;
    private final GoalType activeGoal;
    private final boolean useGroupStorage;
    private final boolean collectionistMode;
    private final boolean allowWildernessMethods;
    private final VarietyPreference varietyPreference;

    public PlayerStrategyProfile(
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode)
    {
        this(strategyMode, sessionIntent, questTolerance, activeGoal,
                useGroupStorage, collectionistMode, false,
                VarietyPreference.BALANCED);
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
        this(strategyMode, sessionIntent, questTolerance, activeGoal,
                useGroupStorage, collectionistMode, allowWildernessMethods,
                VarietyPreference.BALANCED);
    }

    public PlayerStrategyProfile(
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            boolean allowWildernessMethods,
            VarietyPreference varietyPreference)
    {
        this.strategyMode = strategyMode == null ? StrategyMode.BALANCED : strategyMode;
        this.sessionIntent = sessionIntent == null ? SessionIntent.PICK_FOR_ME : sessionIntent;
        this.questTolerance = questTolerance == null ? QuestTolerance.NORMAL : questTolerance;
        this.activeGoal = activeGoal == null ? GoalType.MAX : activeGoal;
        this.useGroupStorage = useGroupStorage;
        this.collectionistMode = collectionistMode;
        this.allowWildernessMethods = allowWildernessMethods;
        this.varietyPreference = varietyPreference == null
                ? VarietyPreference.BALANCED : varietyPreference;
    }

    public static PlayerStrategyProfile fromConfig(OsrsStrategistConfig config)
    {
        return new PlayerStrategyProfile(
                config.strategyMode(), config.sessionIntent(),
                config.questTolerance(), config.activeGoal(),
                config.useGroupStorage(), config.collectionistMode(),
                config.allowWildernessMethods(), config.varietyPreference()
        );
    }

    public StrategyMode getStrategyMode()
    {
        return strategyMode == null ? StrategyMode.BALANCED : strategyMode;
    }

    public SessionIntent getSessionIntent()
    {
        return sessionIntent == null ? SessionIntent.PICK_FOR_ME : sessionIntent;
    }

    public QuestTolerance getQuestTolerance()
    {
        return questTolerance == null ? QuestTolerance.NORMAL : questTolerance;
    }

    public GoalType getActiveGoal()
    {
        return activeGoal == null ? GoalType.MAX : activeGoal;
    }

    public boolean isUseGroupStorage() { return useGroupStorage; }
    public boolean isCollectionistMode() { return collectionistMode; }
    public boolean isAllowWildernessMethods() { return allowWildernessMethods; }

    public VarietyPreference getVarietyPreference()
    {
        return varietyPreference == null
                ? VarietyPreference.BALANCED : varietyPreference;
    }
}
