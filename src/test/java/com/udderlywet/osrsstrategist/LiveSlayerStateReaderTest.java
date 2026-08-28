package com.udderlywet.osrsstrategist;

import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
