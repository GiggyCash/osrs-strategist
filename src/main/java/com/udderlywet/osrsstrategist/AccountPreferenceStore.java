package com.udderlywet.osrsstrategist;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Persists learned Strategist preferences in RuneLite's RuneScape-profile
 * configuration so each character keeps its own recommendation personality.
 */
@Singleton
public class AccountPreferenceStore
{
    private static final String GROUP = "osrs-strategist-profile";
    private static final String PREFERENCES_KEY = "preferences";
    private static final Type PREFERENCE_MAP_TYPE =
            new TypeToken<Map<String, Double>>() { }.getType();

    private final ConfigManager configManager;
    private final Gson gson;

    @Inject
    public AccountPreferenceStore(
            ConfigManager configManager,
            Gson gson)
    {
        this.configManager = configManager;
        this.gson = gson;
    }

    public String getActiveProfileKey()
    {
        return configManager.getRSProfileKey();
    }

    public void loadInto(PreferenceProfile profile)
    {
        profile.clear();

        if (getActiveProfileKey() == null)
        {
            return;
        }

        String json = configManager.getRSProfileConfiguration(
                GROUP,
                PREFERENCES_KEY
        );

        if (json == null || json.trim().isEmpty())
        {
            return;
        }

        Map<String, Double> stored =
                gson.fromJson(json, PREFERENCE_MAP_TYPE);

        if (stored == null)
        {
            stored = Collections.emptyMap();
        }

        profile.replaceAll(stored);
    }

    public void save(PreferenceProfile profile)
    {
        // Do not require an existing RS profile key here.
        // RuneLite's setRSProfileConfiguration() creates the per-character
        // profile on the first write when the player is logged in.
        configManager.setRSProfileConfiguration(
                GROUP,
                PREFERENCES_KEY,
                gson.toJson(profile.snapshot())
        );
    }
}
