package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
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
    private static final int MORTIMER_MASTER_ID = 10;

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
        List<SlayerTaskOffer> offers = amount <= 0
                ? readMortimerOffers() : java.util.Collections.emptyList();
        if (!offers.isEmpty()) masterId = MORTIMER_MASTER_ID;
        String masterName = masterName(masterId);
        SlayerRewardSnapshot rewards = readRewards();
        Integer streak = streak(masterId);
        int questPoints = Math.max(0, client.getVarpValue(VarPlayerID.QP));
        boolean lumbridgeElite = client.getVarbitValue(
                VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) > 0;
        int blockCapacity = masterId == MORTIMER_MASTER_ID ? 2
                : SlayerPointEconomy.blockCapacity(questPoints, lumbridgeElite);
        Integer occupiedBlockSlots = occupiedBlockSlots(masterId, blockCapacity);
        boolean mortimerIntroduced = client.getVarbitValue(
                VarbitID.MORTIMER_INTRODUCTION) > 0;
        if (amount <= 0)
        {
            cached = new SlayerSnapshot(
                    null, 0, masterName, null, points, streak, questPoints,
                    blockCapacity, occupiedBlockSlots, rewards, offers,
                    mortimerIntroduced,
                    RecommendationConfidence.VERIFIED);
            cachedTick = tick;
            return cached;
        }

        try
        {
            String taskName = taskName(
                    client.getVarpValue(VarPlayerID.SLAYER_TARGET),
                    client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
            if (taskName == null) return unresolved(amount, masterName,
                    points, streak, questPoints, blockCapacity, rewards,
                    mortimerIntroduced, tick);

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
                    java.util.Collections.emptyList(),
                    mortimerIntroduced,
                    RecommendationConfidence.VERIFIED);
            cachedTick = tick;
            return cached;
        }
        catch (RuntimeException ex)
        {
            return unresolved(amount, masterName, points, streak, questPoints,
                    blockCapacity, rewards, mortimerIntroduced, tick);
        }
    }

    public void clear()
    {
        cachedTick = -1;
        cached = null;
    }

    private SlayerSnapshot unresolved(int amount, String masterName,
            int points, Integer streak, int questPoints, int blockCapacity,
            SlayerRewardSnapshot rewards, boolean mortimerIntroduced, int tick)
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
                java.util.Collections.emptyList(),
                mortimerIntroduced,
                RecommendationConfidence.CHECK_NEEDED);
        cachedTick = tick;
        return cached;
    }

    private Integer streak(int masterId)
    {
        if (masterId == MORTIMER_MASTER_ID)
        {
            // RuneLite exposes Mortimer's live choices/modifiers but no public
            // separate completed-task counter. Never substitute normal streak.
            return null;
        }
        return Math.max(0, client.getVarbitValue(
                masterId == KRYSTILIA_MASTER_ID
                        ? VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED
                        : VarbitID.SLAYER_TASKS_COMPLETED));
    }

    private List<SlayerTaskOffer> readMortimerOffers()
    {
        List<SlayerTaskOffer> offers = new ArrayList<>();
        addOffer(offers, VarbitID.SLAYER_CHOOSE_TASK_1,
                VarbitID.SLAYER_CHOOSE_TASK_1_BOSS_ID,
                VarbitID.SLAYER_CHOOSE_TASK_1_MODIFIER_ID,
                VarbitID.SLAYER_CHOOSE_TASK_1_MODIFIER_VALUE,
                VarbitID.SLAYER_CHOOSE_TASK_1_MODIFIER_NEGATIVE);
        addOffer(offers, VarbitID.SLAYER_CHOOSE_TASK_2,
                VarbitID.SLAYER_CHOOSE_TASK_2_BOSS_ID,
                VarbitID.SLAYER_CHOOSE_TASK_2_MODIFIER_ID,
                VarbitID.SLAYER_CHOOSE_TASK_2_MODIFIER_VALUE,
                VarbitID.SLAYER_CHOOSE_TASK_2_MODIFIER_NEGATIVE);
        addOffer(offers, VarbitID.SLAYER_CHOOSE_TASK_3,
                VarbitID.SLAYER_CHOOSE_TASK_3_BOSS_ID,
                VarbitID.SLAYER_CHOOSE_TASK_3_MODIFIER_ID,
                VarbitID.SLAYER_CHOOSE_TASK_3_MODIFIER_VALUE,
                VarbitID.SLAYER_CHOOSE_TASK_3_MODIFIER_NEGATIVE);
        return offers;
    }

    private void addOffer(List<SlayerTaskOffer> offers, int taskVarbit,
            int bossVarbit, int modifierVarbit, int valueVarbit,
            int negativeVarbit)
    {
        int taskId = client.getVarbitValue(taskVarbit);
        if (taskId <= 0) return;
        String task = null;
        String modifier = null;
        int value = client.getVarbitValue(valueVarbit);
        boolean negative = client.getVarbitValue(negativeVarbit) > 0;
        try
        {
            task = taskName(taskId, client.getVarbitValue(bossVarbit));
            modifier = modifierName(
                    client.getVarbitValue(modifierVarbit));
        }
        catch (RuntimeException ignored)
        {
            // Preserve the option as unresolved. Omitting it could make a
            // decoded alternative look best when the hidden option is better.
        }
        offers.add(new SlayerTaskOffer(task, modifier, value, negative));
    }

    private String taskName(int taskId, int bossId)
    {
        int taskRow;
        if (taskId == BOSS_TASK_ID)
        {
            var rows = client.getDBRowsByValue(
                    DBTableID.SlayerTaskSublist.ID,
                    DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
                    0, bossId);
            if (rows.isEmpty()) return null;
            taskRow = (Integer) client.getDBTableField(rows.get(0),
                    DBTableID.SlayerTaskSublist.COL_TASK, 0)[0];
        }
        else
        {
            var rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID,
                    DBTableID.SlayerTask.COL_ID, 0, taskId);
            if (rows.isEmpty()) return null;
            taskRow = rows.get(0);
        }
        Object[] values = client.getDBTableField(taskRow,
                DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
        return values == null || values.length == 0
                ? null : (String) values[0];
    }

    private String modifierName(int modifierId)
    {
        var rows = client.getDBRowsByValue(DBTableID.SlayerModifiers.ID,
                DBTableID.SlayerModifiers.COL_ID, 0, modifierId);
        if (rows.isEmpty()) return null;
        Object[] values = client.getDBTableField(rows.get(0),
                DBTableID.SlayerModifiers.COL_NAME, 0);
        return values == null || values.length == 0
                ? null : (String) values[0];
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
            case 10: return "Mortimer";
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
            case 10:
                return new int[]{VarbitID.SLAYER_BLOCKED_MORTIMER_1,
                        VarbitID.SLAYER_BLOCKED_MORTIMER_2};
            default:
                return null;
        }
    }
}
