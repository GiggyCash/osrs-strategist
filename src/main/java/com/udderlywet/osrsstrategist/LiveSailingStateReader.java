package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarPlayerID;

/** Reads only Sailing progression that RuneLite exposes as stable live state. */
@Singleton
public class LiveSailingStateReader
{
    private static final int[] PORT_TASK_VARPS = {
        VarPlayerID.PORT_TASKS_0, VarPlayerID.PORT_TASKS_1,
        VarPlayerID.PORT_TASKS_2, VarPlayerID.PORT_TASKS_3,
        VarPlayerID.PORT_TASKS_4
    };
    private static final int[] BOAT_DATA_VARPS = {
        VarPlayerID.SAILING_BOAT_1_DATA, VarPlayerID.SAILING_BOAT_2_DATA,
        VarPlayerID.SAILING_BOAT_3_DATA, VarPlayerID.SAILING_BOAT_4_DATA,
        VarPlayerID.SAILING_BOAT_5_DATA
    };

    private final Client client;
    private int cachedTick = -1;
    private SailingSnapshot cached;

    @Inject
    public LiveSailingStateReader(Client client)
    {
        this.client = client;
    }

    public SailingSnapshot read(QuestSnapshot quests)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            cachedTick = -1;
            cached = null;
            return null;
        }
        int tick = client.getTickCount();
        if (cached != null && cachedTick == tick) return cached;

        Set<String> ports = new HashSet<>();
        Set<String> activities = new HashSet<>();
        if (quests != null
                && quests.statusOf("Pandemonium") == QuestStatus.COMPLETE)
        {
            // Pandemonium deterministically grants the starter raft, Captain's
            // log access, and the route between its island and Port Sarim.
            ports.add(SailingSnapshot.PORT_SARIM);
            ports.add(SailingSnapshot.PORT_PANDEMONIUM);
            activities.add(SailingSnapshot.ACTIVITY_COURIER);
            activities.add(SailingSnapshot.ACTIVITY_SEA_CHARTING);
        }
        if (anyPositive(BOAT_DATA_VARPS))
            activities.add(SailingSnapshot.ACTIVITY_BOAT_OWNED);
        if (anyPositive(PORT_TASK_VARPS))
            activities.add(SailingSnapshot.ACTIVITY_ACTIVE_PORT_TASK);
        addIfPositive(activities,
                VarPlayerID.SAILING_BT_TRIAL_TEMPOR_TANTRUM_COMPLETED,
                SailingSnapshot.TRIAL_TEMPOR_COMPLETE);
        addIfPositive(activities,
                VarPlayerID.SAILING_BT_TRIAL_JUBBLY_JIVE_COMPLETED,
                SailingSnapshot.TRIAL_JUBBLY_COMPLETE);
        addIfPositive(activities,
                VarPlayerID.SAILING_BT_TRIAL_GWENITH_GLIDE_COMPLETED,
                SailingSnapshot.TRIAL_GWENITH_COMPLETE);

        cached = new SailingSnapshot(ports, activities,
                Confidence.VERIFIED);
        cachedTick = tick;
        return cached;
    }

    private boolean anyPositive(int[] varps)
    {
        for (int varp : varps)
            if (client.getVarpValue(varp) > 0) return true;
        return false;
    }

    private void addIfPositive(Set<String> activities, int varp, String id)
    {
        if (client.getVarpValue(varp) > 0) activities.add(id);
    }
}
