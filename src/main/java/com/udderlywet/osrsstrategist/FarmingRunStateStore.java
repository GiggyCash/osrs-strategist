package com.udderlywet.osrsstrategist;

import com.google.gson.Gson;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/** Per-character memory of directly observed herb/tree patch state. */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class FarmingRunStateStore
{
    static final String GROUP = "osrs-strategist-profile";
    private static final String KEY = "farmingRunStates";
    private final ConfigManager configManager;
    private final Gson gson;
    private final Map<String, ObservedFarmingPatchState> states = new HashMap<>();
    private String loadedProfileKey;

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
        var previous = states.get(patchId);
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
        var active = configManager.getRSProfileKey();
        if (Objects.equals(loadedProfileKey, active) && active != null) return;
        states.clear();
        if (active != null)
        {
            var json = configManager.getRSProfileConfiguration(GROUP, KEY);
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
