package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Reads the same authoritative Slayer assignment state exposed to RuneLite.
 *
 * <p>This avoids scraping chat text or relying on the user to type the current
 * task. Task count, task row, area, and points all come from live client state.
 */
@Singleton
public class LiveSlayerStateReader
{
    private static final int BOSS_TASK_ID = 98;
    // RuneLite's own Slayer plugin uses value 7 to select the separate
    // Krystilia streak. No other numeric master mapping is inferred here.
    private static final int KRYSTILIA_MASTER_ID = 7;

    private final Client client;
    private int cachedTick = -1;
    private SlayerSnapshot cached;

    @Inject
    public LiveSlayerStateReader(Client client)
    {
        this.client = client;
    }

    public SlayerSnapshot read()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            cachedTick = -1;
            cached = null;
            return null;
        }

        int tick = client.getTickCount();
        if (cached != null && cachedTick == tick)
        {
            return cached;
        }

        int amount = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
        int points = Math.max(0, client.getVarbitValue(VarbitID.SLAYER_POINTS));
        int masterId = client.getVarbitValue(VarbitID.SLAYER_MASTER);
        int streak = Math.max(0, client.getVarbitValue(
                masterId == KRYSTILIA_MASTER_ID
                        ? VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED
                        : VarbitID.SLAYER_TASKS_COMPLETED));
        int questPoints = Math.max(0, client.getVarpValue(VarPlayerID.QP));
        boolean lumbridgeElite = client.getVarbitValue(
                VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) > 0;
        int blockCapacity = SlayerPointEconomy.blockCapacity(
                questPoints, lumbridgeElite);
        if (amount <= 0)
        {
            cached = new SlayerSnapshot(
                    null, 0, null, null, points, streak, questPoints,
                    blockCapacity, null,
                    RecommendationConfidence.VERIFIED);
            cachedTick = tick;
            return cached;
        }

        try
        {
            int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
            int taskRow;
            if (taskId == BOSS_TASK_ID)
            {
                var rows = client.getDBRowsByValue(
                        DBTableID.SlayerTaskSublist.ID,
                        DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
                        0,
                        client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
                if (rows.isEmpty()) return unresolved(amount, points, streak,
                        questPoints, blockCapacity, tick);
                taskRow = (Integer) client.getDBTableField(
                        rows.get(0),
                        DBTableID.SlayerTaskSublist.COL_TASK,
                        0)[0];
            }
            else
            {
                var rows = client.getDBRowsByValue(
                        DBTableID.SlayerTask.ID,
                        DBTableID.SlayerTask.COL_ID,
                        0,
                        taskId);
                if (rows.isEmpty()) return unresolved(amount, points, streak,
                        questPoints, blockCapacity, tick);
                taskRow = rows.get(0);
            }

            String taskName = (String) client.getDBTableField(
                    taskRow,
                    DBTableID.SlayerTask.COL_NAME_UPPERCASE,
                    0)[0];

            String taskLocation = null;
            int areaId = client.getVarpValue(VarPlayerID.SLAYER_AREA);
            if (areaId > 0)
            {
                var rows = client.getDBRowsByValue(
                        DBTableID.SlayerArea.ID,
                        DBTableID.SlayerArea.COL_AREA_ID,
                        0,
                        areaId);
                if (!rows.isEmpty())
                {
                    taskLocation = (String) client.getDBTableField(
                            rows.get(0),
                            DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER,
                            0)[0];
                }
            }

            cached = new SlayerSnapshot(
                    taskName,
                    amount,
                    null,
                    taskLocation,
                    points,
                    streak,
                    questPoints,
                    blockCapacity,
                    null,
                    RecommendationConfidence.VERIFIED);
            cachedTick = tick;
            return cached;
        }
        catch (RuntimeException ex)
        {
            return unresolved(amount, points, streak, questPoints,
                    blockCapacity, tick);
        }
    }

    public void clear()
    {
        cachedTick = -1;
        cached = null;
    }

    private SlayerSnapshot unresolved(int amount, int points, int streak,
            int questPoints, int blockCapacity, int tick)
    {
        cached = new SlayerSnapshot(
                null,
                amount,
                null,
                null,
                points,
                streak,
                questPoints,
                blockCapacity,
                null,
                RecommendationConfidence.CHECK_NEEDED);
        cachedTick = tick;
        return cached;
    }
}
