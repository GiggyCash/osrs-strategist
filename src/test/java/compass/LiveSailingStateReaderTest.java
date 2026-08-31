package compass;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarPlayerID;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LiveSailingStateReaderTest
{
    @Test
    public void liveVarplayersAndQuestProveOnlyObservedSailingState()
    {
        Map<Integer, Integer> varps = new HashMap<>();
        varps.put(VarPlayerID.SAILING_BOAT_2_DATA, 41);
        varps.put(VarPlayerID.PORT_TASKS_3, 2);
        varps.put(VarPlayerID.SAILING_BT_TRIAL_TEMPOR_TANTRUM_COMPLETED, 1);
        LiveSailingStateReader reader = new LiveSailingStateReader(
                client(GameState.LOGGED_IN, varps));

        SailingSnapshot sailing = reader.read(completedPandemonium());

        assertTrue(sailing.hasPort(SailingSnapshot.PORT_SARIM));
        assertTrue(sailing.hasPort(SailingSnapshot.PORT_PANDEMONIUM));
        assertTrue(sailing.hasActivity(SailingSnapshot.ACTIVITY_COURIER));
        assertTrue(sailing.hasActivity(SailingSnapshot.ACTIVITY_BOAT_OWNED));
        assertTrue(sailing.hasActivity(
                SailingSnapshot.ACTIVITY_ACTIVE_PORT_TASK));
        assertTrue(sailing.hasActivity(SailingSnapshot.TRIAL_TEMPOR_COMPLETE));
    }

    @Test
    public void loggedOutStateIsNotEvidence()
    {
        assertNull(new LiveSailingStateReader(
                client(GameState.LOGIN_SCREEN, Collections.emptyMap()))
                .read(completedPandemonium()));
    }

    private static QuestSnapshot completedPandemonium()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Pandemonium", QuestStatus.COMPLETE);
        return new QuestSnapshot(quests);
    }

    private static Client client(GameState gameState,
            Map<Integer, Integer> varps)
    {
        return (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
                new Class<?>[]{Client.class}, (proxy, method, args) ->
                {
                    switch (method.getName())
                    {
                        case "getGameState": return gameState;
                        case "getTickCount": return 17;
                        case "getVarpValue":
                            return varps.getOrDefault((Integer) args[0], 0);
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
