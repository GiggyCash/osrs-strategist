package com.udderlywet.osrsstrategist;

import java.util.EnumSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;

/** Reads the game's six Combat Achievement reward-tier completion varbits. */
@Singleton
public class LiveCombatAchievementReader
{
    private final Client client;

    @Inject
    public LiveCombatAchievementReader(Client client)
    {
        this.client = client;
    }

    public CombatAchievementSnapshot read(CombatAchievementSnapshot observed)
    {
        if (client.getGameState() != GameState.LOGGED_IN) return observed;

        Set<CombatAchievementTier> tiers = EnumSet.noneOf(CombatAchievementTier.class);
        addIfComplete(tiers, CombatAchievementTier.EASY,
                Varbits.COMBAT_ACHIEVEMENT_TIER_EASY);
        addIfComplete(tiers, CombatAchievementTier.MEDIUM,
                Varbits.COMBAT_ACHIEVEMENT_TIER_MEDIUM);
        addIfComplete(tiers, CombatAchievementTier.HARD,
                Varbits.COMBAT_ACHIEVEMENT_TIER_HARD);
        addIfComplete(tiers, CombatAchievementTier.ELITE,
                Varbits.COMBAT_ACHIEVEMENT_TIER_ELITE);
        addIfComplete(tiers, CombatAchievementTier.MASTER,
                Varbits.COMBAT_ACHIEVEMENT_TIER_MASTER);
        addIfComplete(tiers, CombatAchievementTier.GRANDMASTER,
                Varbits.COMBAT_ACHIEVEMENT_TIER_GRANDMASTER);

        int minimumPoints = 0;
        for (CombatAchievementTier tier : tiers)
            minimumPoints = Math.max(minimumPoints, tier.getRewardPoints());
        int observedPoints = observed == null ? 0 : observed.getEarnedPoints();
        int observedTasks = observed == null ? 0 : observed.getCompletedTasks();
        return new CombatAchievementSnapshot(
                observedTasks,
                Math.max(minimumPoints, observedPoints),
                tiers
        );
    }

    private void addIfComplete(Set<CombatAchievementTier> tiers,
            CombatAchievementTier tier, int varbit)
    {
        // RuneLite documents value 2 as completed. Accept >= 2 so future
        // additional completion states do not regress an already-earned tier.
        if (client.getVarbitValue(varbit) >= 2) tiers.add(tier);
    }
}
