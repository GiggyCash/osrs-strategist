package com.udderlywet.osrsstrategist;

import com.google.gson.Gson;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/** Per-character memory of directly observed herb/tree patch state. */
@Singleton
public class FarmingRunStateStore
{
    static final String GROUP = "osrs-strategist-profile";
    private static final String KEY = "farmingRunStates";
    private final ConfigManager configManager;
    private final Gson gson;
    private final Map<String, ObservedFarmingPatchState> states = new HashMap<>();
    private String loadedProfileKey;

    @Inject
    public FarmingRunStateStore(ConfigManager configManager, Gson gson)
    {
        this.configManager = configManager;
        this.gson = gson;
    }

    public synchronized FarmingRunSnapshot snapshot()
    {
        syncProfile();
        return new FarmingRunSnapshot(states);
    }

    public synchronized boolean remember(
            String patchId,
            FarmingPatchCycleState state)
    {
        if (patchId == null || state == null || state == FarmingPatchCycleState.UNKNOWN)
        {
            return false;
        }
        syncProfile();
        ObservedFarmingPatchState previous = states.get(patchId);
        if (previous != null && previous.getState() == state)
        {
            return false;
        }
        states.put(patchId, new ObservedFarmingPatchState(
                state, System.currentTimeMillis()));
        configManager.setRSProfileConfiguration(
                GROUP, KEY, gson.toJson(states));
        loadedProfileKey = configManager.getRSProfileKey();
        return true;
    }

    public synchronized void clearCacheForAccountChange()
    {
        loadedProfileKey = null;
        states.clear();
    }

    private void syncProfile()
    {
        String active = configManager.getRSProfileKey();
        if (Objects.equals(loadedProfileKey, active) && active != null) return;
        states.clear();
        if (active != null)
        {
            String json = configManager.getRSProfileConfiguration(GROUP, KEY);
            if (json != null && !json.trim().isEmpty())
            {
                Map<String, ObservedFarmingPatchState> stored =
                        ProfileJsonCodec.farmingStates(gson, json);
                if (stored != null) states.putAll(stored);
            }
        }
        loadedProfileKey = active;
    }
}
