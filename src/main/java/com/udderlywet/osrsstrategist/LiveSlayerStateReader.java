package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.Map;
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
        String masterName = masterName(masterId);
        SlayerRewardSnapshot rewards = readRewards();
        int streak = Math.max(0, client.getVarbitValue(
                masterId == KRYSTILIA_MASTER_ID
                        ? VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED
                        : VarbitID.SLAYER_TASKS_COMPLETED));
        int questPoints = Math.max(0, client.getVarpValue(VarPlayerID.QP));
        boolean lumbridgeElite = client.getVarbitValue(
                VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) > 0;
        int blockCapacity = SlayerPointEconomy.blockCapacity(
                questPoints, lumbridgeElite);
        Integer occupiedBlockSlots = occupiedBlockSlots(masterId, blockCapacity);
        if (amount <= 0)
        {
            cached = new SlayerSnapshot(
                    null, 0, masterName, null, points, streak, questPoints,
                    blockCapacity, occupiedBlockSlots, rewards,
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
                if (rows.isEmpty()) return unresolved(amount, masterName,
                        points, streak, questPoints, blockCapacity, rewards, tick);
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
                if (rows.isEmpty()) return unresolved(amount, masterName,
                        points, streak, questPoints, blockCapacity, rewards, tick);
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
                    masterName,
                    taskLocation,
                    points,
                    streak,
                    questPoints,
                    blockCapacity,
                    occupiedBlockSlots,
                    rewards,
                    RecommendationConfidence.VERIFIED);
            cachedTick = tick;
            return cached;
        }
        catch (RuntimeException ex)
        {
            return unresolved(amount, masterName, points, streak, questPoints,
                    blockCapacity, rewards, tick);
        }
    }

    public void clear()
    {
        cachedTick = -1;
        cached = null;
    }

    private SlayerSnapshot unresolved(int amount, String masterName,
            int points, int streak, int questPoints, int blockCapacity,
            SlayerRewardSnapshot rewards, int tick)
    {
        cached = new SlayerSnapshot(
                null,
                amount,
                masterName,
                null,
                points,
                streak,
                questPoints,
                blockCapacity,
                null,
                rewards,
                RecommendationConfidence.CHECK_NEEDED);
        cachedTick = tick;
        return cached;
    }

    private SlayerRewardSnapshot readRewards()
    {
        Map<SlayerReward, CapabilityState> states =
                new EnumMap<>(SlayerReward.class);
        for (SlayerReward reward : SlayerReward.values())
            states.put(reward, client.getVarbitValue(reward.getVarbitId()) > 0
                    ? CapabilityState.VERIFIED : CapabilityState.BLOCKED);
        return new SlayerRewardSnapshot(states);
    }

    static String masterName(int masterId)
    {
        switch (masterId)
        {
            case 1: return "Turael/Aya";
            case 2: return "Mazchna/Achtryn";
            case 3: return "Vannaka";
            case 4: return "Chaeldar";
            case 5: return "Duradel/Kuradal";
            case 6: return "Nieve/Steve";
            case 7: return "Krystilia";
            case 8: return "Konar quo Maten";
            case 9: return "Spria";
            default: return null;
        }
    }

    private Integer occupiedBlockSlots(int masterId, int capacity)
    {
        int[] slots = blockVarbits(masterId);
        if (slots == null) return null;
        int occupied = 0;
        int visible = Math.min(capacity, slots.length);
        for (int i = 0; i < visible; i++)
            if (client.getVarbitValue(slots[i]) > 0) occupied++;
        return occupied;
    }

    private static int[] blockVarbits(int masterId)
    {
        switch (masterId)
        {
            case 1:
            case 9:
                return new int[]{VarbitID.SLAYER_BLOCKED_TURAEL_1,
                        VarbitID.SLAYER_BLOCKED_TURAEL_2,
                        VarbitID.SLAYER_BLOCKED_TURAEL_3,
                        VarbitID.SLAYER_BLOCKED_TURAEL_4,
                        VarbitID.SLAYER_BLOCKED_TURAEL_5,
                        VarbitID.SLAYER_BLOCKED_TURAEL_6,
                        VarbitID.SLAYER_BLOCKED_TURAEL_DIARY};
            case 2:
                return new int[]{VarbitID.SLAYER_BLOCKED_MAZCHNA_1,
                        VarbitID.SLAYER_BLOCKED_MAZCHNA_2,
                        VarbitID.SLAYER_BLOCKED_MAZCHNA_3,
                        VarbitID.SLAYER_BLOCKED_MAZCHNA_4,
                        VarbitID.SLAYER_BLOCKED_MAZCHNA_5,
                        VarbitID.SLAYER_BLOCKED_MAZCHNA_6,
                        VarbitID.SLAYER_BLOCKED_MAZCHNA_DIARY};
            case 3:
                return new int[]{VarbitID.SLAYER_BLOCKED_VANNAKA_1,
                        VarbitID.SLAYER_BLOCKED_VANNAKA_2,
                        VarbitID.SLAYER_BLOCKED_VANNAKA_3,
                        VarbitID.SLAYER_BLOCKED_VANNAKA_4,
                        VarbitID.SLAYER_BLOCKED_VANNAKA_5,
                        VarbitID.SLAYER_BLOCKED_VANNAKA_6,
                        VarbitID.SLAYER_BLOCKED_VANNAKA_DIARY};
            case 4:
                return new int[]{VarbitID.SLAYER_BLOCKED_CHAELDAR_1,
                        VarbitID.SLAYER_BLOCKED_CHAELDAR_2,
                        VarbitID.SLAYER_BLOCKED_CHAELDAR_3,
                        VarbitID.SLAYER_BLOCKED_CHAELDAR_4,
                        VarbitID.SLAYER_BLOCKED_CHAELDAR_5,
                        VarbitID.SLAYER_BLOCKED_CHAELDAR_6,
                        VarbitID.SLAYER_BLOCKED_CHAELDAR_DIARY};
            case 5:
                return new int[]{VarbitID.SLAYER_BLOCKED_DURADEL_1,
                        VarbitID.SLAYER_BLOCKED_DURADEL_2,
                        VarbitID.SLAYER_BLOCKED_DURADEL_3,
                        VarbitID.SLAYER_BLOCKED_DURADEL_4,
                        VarbitID.SLAYER_BLOCKED_DURADEL_5,
                        VarbitID.SLAYER_BLOCKED_DURADEL_6,
                        VarbitID.SLAYER_BLOCKED_DURADEL_DIARY};
            case 6:
                return new int[]{VarbitID.SLAYER_BLOCKED_NIEVE_1,
                        VarbitID.SLAYER_BLOCKED_NIEVE_2,
                        VarbitID.SLAYER_BLOCKED_NIEVE_3,
                        VarbitID.SLAYER_BLOCKED_NIEVE_4,
                        VarbitID.SLAYER_BLOCKED_NIEVE_5,
                        VarbitID.SLAYER_BLOCKED_NIEVE_6,
                        VarbitID.SLAYER_BLOCKED_NIEVE_DIARY};
            case 7:
                return new int[]{VarbitID.SLAYER_BLOCKED_KRYSTILIA_1,
                        VarbitID.SLAYER_BLOCKED_KRYSTILIA_2,
                        VarbitID.SLAYER_BLOCKED_KRYSTILIA_3,
                        VarbitID.SLAYER_BLOCKED_KRYSTILIA_4,
                        VarbitID.SLAYER_BLOCKED_KRYSTILIA_5,
                        VarbitID.SLAYER_BLOCKED_KRYSTILIA_6,
                        VarbitID.SLAYER_BLOCKED_KRYSTILIA_DIARY};
            case 8:
                return new int[]{VarbitID.SLAYER_BLOCKED_KONAR_1,
                        VarbitID.SLAYER_BLOCKED_KONAR_2,
                        VarbitID.SLAYER_BLOCKED_KONAR_3,
                        VarbitID.SLAYER_BLOCKED_KONAR_4,
                        VarbitID.SLAYER_BLOCKED_KONAR_5,
                        VarbitID.SLAYER_BLOCKED_KONAR_6,
                        VarbitID.SLAYER_BLOCKED_KONAR_DIARY};
            default:
                return null;
        }
    }
}
