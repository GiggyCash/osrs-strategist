package com.udderlywet.osrsstrategist;

import java.lang.reflect.Proxy;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LiveSlayerStateReaderTest
{
    @Test
    public void observedNoTaskIncludesStreakAndBlockCapacity()
    {
        LiveSlayerStateReader reader = new LiveSlayerStateReader(client(
                0, 275, 190, 49, 3, 0, true));

        SlayerSnapshot snapshot = reader.read();

        assertEquals(SlayerAssignmentState.NO_TASK,
                snapshot.getAssignmentState());
        assertEquals(190, snapshot.getPoints());
        assertEquals(Integer.valueOf(49), snapshot.getTaskStreak());
        assertEquals(Integer.valueOf(275), snapshot.getQuestPoints());
        assertEquals(Integer.valueOf(6), snapshot.getBlockSlotCapacity());
        assertNull(snapshot.getOccupiedBlockSlots());
    }

    @Test
    public void krystiliaUsesSeparateRuneLiteStreakEvidence()
    {
        LiveSlayerStateReader reader = new LiveSlayerStateReader(client(
                0, 300, 50, 123, 8, 7, false));

        SlayerSnapshot snapshot = reader.read();

        assertEquals(Integer.valueOf(8), snapshot.getTaskStreak());
        assertEquals(Integer.valueOf(6), snapshot.getBlockSlotCapacity());
        assertEquals("Krystilia", snapshot.getMasterName());
        assertEquals(Integer.valueOf(0), snapshot.getOccupiedBlockSlots());
        assertEquals(CapabilityState.BLOCKED,
                snapshot.getRewards().stateOf(SlayerReward.BIGGER_AND_BADDER));
    }

    @Test
    public void assigningMasterVarbitUsesVerifiedGameMapping()
    {
        assertNull(LiveSlayerStateReader.masterName(0));
        assertEquals("Turael/Aya", LiveSlayerStateReader.masterName(1));
        assertEquals("Duradel/Kuradal", LiveSlayerStateReader.masterName(5));
        assertEquals("Nieve/Steve", LiveSlayerStateReader.masterName(6));
        assertEquals("Konar quo Maten", LiveSlayerStateReader.masterName(8));
        assertEquals("Spria", LiveSlayerStateReader.masterName(9));
        assertEquals("Mortimer", LiveSlayerStateReader.masterName(10));
    }

    @Test
    public void mortimerUsesSeparateBlocksAndDoesNotBorrowNormalStreak()
    {
        LiveSlayerStateReader reader = new LiveSlayerStateReader(client(
                0, 300, 250, 88, 4, 10, true));

        SlayerSnapshot snapshot = reader.read();

        assertEquals("Mortimer", snapshot.getMasterName());
        assertNull(snapshot.getTaskStreak());
        assertEquals(Integer.valueOf(2), snapshot.getBlockSlotCapacity());
        assertEquals(Integer.valueOf(0), snapshot.getOccupiedBlockSlots());
    }

    @Test
    public void liveMortimerOptionsDecodeTasksAndModifiersFromRuneLiteData()
    {
        SlayerSnapshot snapshot = new LiveSlayerStateReader(
                mortimerChoiceClient()).read();

        assertEquals(SlayerAssignmentState.CHOICE_PENDING,
                snapshot.getAssignmentState());
        assertEquals(2, snapshot.getTaskOffers().size());
        assertEquals("Dust devils",
                snapshot.getTaskOffers().get(0).getTaskName());
        assertEquals("Slayer XP",
                snapshot.getTaskOffers().get(0).getModifierName());
        assertEquals(20, snapshot.getTaskOffers().get(0).getModifierValue());
        assertEquals("Hellhounds",
                snapshot.getTaskOffers().get(1).getTaskName());
        assertTrue(snapshot.isMortimerIntroduced());
    }

    private static Client mortimerChoiceClient()
    {
        return (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
                new Class<?>[]{Client.class}, (proxy, method, args) ->
                {
                    switch (method.getName())
                    {
                        case "getGameState": return GameState.LOGGED_IN;
                        case "getTickCount": return 11;
                        case "getVarpValue": return 0;
                        case "getVarbitValue":
                            int varbit = (Integer) args[0];
                            if (varbit == VarbitID.SLAYER_POINTS) return 250;
                            if (varbit == VarbitID.SLAYER_CHOOSE_TASK_1) return 501;
                            if (varbit == VarbitID.SLAYER_CHOOSE_TASK_2) return 502;
                            if (varbit == VarbitID.SLAYER_CHOOSE_TASK_1_MODIFIER_ID)
                                return 1;
                            if (varbit == VarbitID.SLAYER_CHOOSE_TASK_2_MODIFIER_ID)
                                return 2;
                            if (varbit == VarbitID.SLAYER_CHOOSE_TASK_1_MODIFIER_VALUE)
                                return 20;
                            if (varbit == VarbitID.SLAYER_CHOOSE_TASK_2_MODIFIER_VALUE)
                                return 10;
                            if (varbit == VarbitID.MORTIMER_INTRODUCTION) return 1;
                            return 0;
                        case "getDBRowsByValue":
                            int lookup = (Integer) args[3];
                            if (lookup == 501) return Collections.singletonList(1001);
                            if (lookup == 502) return Collections.singletonList(1002);
                            if (lookup == 1) return Collections.singletonList(2001);
                            if (lookup == 2) return Collections.singletonList(2002);
                            return Collections.emptyList();
                        case "getDBTableField":
                            int row = (Integer) args[0];
                            if (row == 1001) return new Object[]{"Dust devils"};
                            if (row == 1002) return new Object[]{"Hellhounds"};
                            if (row == 2001) return new Object[]{"Slayer XP"};
                            if (row == 2002) return new Object[]{"Quantity"};
                            return new Object[0];
                        default:
                            Class<?> type = method.getReturnType();
                            if (type == boolean.class) return false;
                            if (type == int.class) return 0;
                            if (type == long.class) return 0L;
                            return null;
                    }
                });
    }

    private static Client client(int taskCount, int questPoints, int points,
            int normalStreak, int wildernessStreak, int masterId,
            boolean lumbridgeElite)
    {
        return (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
                new Class<?>[]{Client.class}, (proxy, method, args) ->
                {
                    switch (method.getName())
                    {
                        case "getGameState": return GameState.LOGGED_IN;
                        case "getTickCount": return 10;
                        case "getVarpValue":
                            int varp = (Integer) args[0];
                            if (varp == VarPlayerID.SLAYER_COUNT) return taskCount;
                            if (varp == VarPlayerID.QP) return questPoints;
                            return 0;
                        case "getVarbitValue":
                            int varbit = (Integer) args[0];
                            if (varbit == VarbitID.SLAYER_POINTS) return points;
                            if (varbit == VarbitID.SLAYER_MASTER) return masterId;
                            if (varbit == VarbitID.SLAYER_TASKS_COMPLETED)
                                return normalStreak;
                            if (varbit == VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED)
                                return wildernessStreak;
                            if (varbit == VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE)
                                return lumbridgeElite ? 1 : 0;
                            return 0;
                        default:
                            Class<?> type = method.getReturnType();
                            if (type == boolean.class) return false;
                            if (type == int.class) return 0;
                            if (type == long.class) return 0L;
                            return null;
                    }
                });
    }
}
