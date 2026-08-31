package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarbitID;

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
                VarbitID.CA_TIER_STATUS_EASY);
        addIfComplete(tiers, CombatAchievementTier.MEDIUM,
                VarbitID.CA_TIER_STATUS_MEDIUM);
        addIfComplete(tiers, CombatAchievementTier.HARD,
                VarbitID.CA_TIER_STATUS_HARD);
        addIfComplete(tiers, CombatAchievementTier.ELITE,
                VarbitID.CA_TIER_STATUS_ELITE);
        addIfComplete(tiers, CombatAchievementTier.MASTER,
                VarbitID.CA_TIER_STATUS_MASTER);
        addIfComplete(tiers, CombatAchievementTier.GRANDMASTER,
                VarbitID.CA_TIER_STATUS_GRANDMASTER);

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
