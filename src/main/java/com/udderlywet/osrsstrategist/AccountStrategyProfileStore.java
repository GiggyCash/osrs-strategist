package com.udderlywet.osrsstrategist;

import com.google.gson.Gson;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Persists explicit strategy settings to RuneLite's per-RuneScape-profile
 * configuration.
 *
 * <p>This is intentionally separate from normal plugin config. Two characters
 * using the same RuneLite installation can therefore have different big goals,
 * strategy styles, quest tolerance, and GIM behavior.</p>
 */
@Singleton
public class AccountStrategyProfileStore
{
    static final String GROUP = "osrs-strategist-profile";
    private static final String KEY = "strategy-settings";

    private final ConfigManager configManager;
    private final Gson gson;

    @Inject
    public AccountStrategyProfileStore(
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

    public PlayerStrategyProfile loadOrDefault(
            PlayerStrategyProfile defaults)
    {
        if (getActiveProfileKey() == null)
        {
            return defaults;
        }

        String json = configManager.getRSProfileConfiguration(
                GROUP,
                KEY
        );

        if (json == null || json.trim().isEmpty())
        {
            return defaults;
        }

        PlayerStrategyProfile stored = gson.fromJson(
                json,
                PlayerStrategyProfile.class
        );

        return stored == null ? defaults : stored.sanitizedForPublicProduct();
    }

    public void save(PlayerStrategyProfile profile)
    {
        if (profile == null)
        {
            return;
        }

        // setRSProfileConfiguration can create the profile on the first write,
        // matching the preference store's behavior.
        configManager.setRSProfileConfiguration(
                GROUP,
                KEY,
                gson.toJson(profile)
        );
    }
}
