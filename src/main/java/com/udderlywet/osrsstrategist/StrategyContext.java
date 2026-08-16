package com.udderlywet.osrsstrategist;

/**
 * Immutable context passed to every strategy subsystem.
 *
 * <p>Centralizing player intent here prevents each future module from reading
 * RuneLite config or profile storage independently. That keeps recommendations
 * deterministic and makes fake-account tests straightforward.</p>
 */
public final class StrategyContext
{
    private final StrategyDataBundle data;
    private final StrategyMode strategyMode;
    private final SessionIntent sessionIntent;
    private final QuestTolerance questTolerance;
    private final GoalType activeGoal;
    private final boolean useGroupStorage;
    private final boolean collectionistMode;
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
        this.data = data;
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
        this.preferenceProfile = preferenceProfile == null
                ? new PreferenceProfile()
                : preferenceProfile;
        this.accountMode = data == null || data.getAccount() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(
                        data.getAccount().getAccountTypeCode()
                );
    }

    public StrategyDataBundle getData() { return data; }
    public StrategyMode getStrategyMode() { return strategyMode; }
    public SessionIntent getSessionIntent() { return sessionIntent; }
    public QuestTolerance getQuestTolerance() { return questTolerance; }
    public GoalType getActiveGoal() { return activeGoal; }
    public boolean isUseGroupStorage() { return useGroupStorage; }
    public boolean isCollectionistMode() { return collectionistMode; }
    public PreferenceProfile getPreferenceProfile() { return preferenceProfile; }
    public AccountMode getAccountMode() { return accountMode; }
}
