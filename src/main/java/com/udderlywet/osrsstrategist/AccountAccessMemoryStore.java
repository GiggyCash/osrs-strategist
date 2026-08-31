package com.udderlywet.osrsstrategist;

import com.google.gson.Gson;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Persists positive access observations per RuneScape character.
 *
 * <p>Only directly observed facts belong here. Inferred access from quests is
 * recalculated from live state instead of being permanently cached as fact.</p>
 */
@Singleton
public class AccountAccessMemoryStore
{
    static final String GROUP = "osrs-strategist-profile";
    private static final String KEY = "accessMemory";
    private final ConfigManager configManager;
    private final Gson gson;
    private final Map<String, Long> memory = new HashMap<>();
    private String loadedProfileKey;

    @Inject
    public AccountAccessMemoryStore(
            ConfigManager configManager,
            Gson gson)
    {
        this.configManager = configManager;
        this.gson = gson;
    }

    public synchronized AccessMemorySnapshot snapshot()
    {
        syncProfile();
        return new AccessMemorySnapshot(memory);
    }

    /**
     * Records a fact only when it is new. Re-saving every game tick would create
     * unnecessary config writes, so repeated observations are intentionally cheap.
     */
    public synchronized boolean remember(String observationKey)
    {
        if (observationKey == null || observationKey.trim().isEmpty())
        {
            return false;
        }

        syncProfile();

        if (memory.containsKey(observationKey))
        {
            return false;
        }

        memory.put(observationKey, System.currentTimeMillis());
        configManager.setRSProfileConfiguration(
                GROUP,
                KEY,
                gson.toJson(memory)
        );
        loadedProfileKey = configManager.getRSProfileKey();
        return true;
    }

    public synchronized void clearCacheForAccountChange()
    {
        loadedProfileKey = null;
        memory.clear();
    }

    private void syncProfile()
    {
        String activeKey = configManager.getRSProfileKey();

        if (Objects.equals(loadedProfileKey, activeKey)
                && loadedProfileKey != null)
        {
            return;
        }

        memory.clear();

        if (activeKey != null)
        {
            String json = configManager.getRSProfileConfiguration(GROUP, KEY);
            if (json != null && !json.trim().isEmpty())
            {
                Map<String, Long> stored = ProfileJsonCodec.longs(gson, json);
                if (stored != null)
                {
                    memory.putAll(stored);
                }
            }
        }

        loadedProfileKey = activeKey;
    }
}
