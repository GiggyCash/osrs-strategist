package com.udderlywet.osrsstrategist;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Persists the player's explicit "never recommend selling this" list per
 * RuneScape character.
 */
@Singleton
public class AccountProtectedItemStore
{
    private static final String GROUP = "osrs-strategist-profile";
    private static final String KEY = "protected-items";
    private static final Type SET_TYPE =
            new TypeToken<Set<Integer>>() { }.getType();

    private final ConfigManager configManager;
    private final Gson gson;

    @Inject
    public AccountProtectedItemStore(
            ConfigManager configManager,
            Gson gson)
    {
        this.configManager = configManager;
        this.gson = gson;
    }

    public void loadInto(ProtectedItemProfile profile)
    {
        if (profile == null)
        {
            return;
        }

        profile.replaceAll(Collections.emptySet());

        if (configManager.getRSProfileKey() == null)
        {
            return;
        }

        String json = configManager.getRSProfileConfiguration(
                GROUP,
                KEY
        );

        if (json == null || json.trim().isEmpty())
        {
            return;
        }

        Set<Integer> stored = gson.fromJson(json, SET_TYPE);
        profile.replaceAll(
                stored == null ? Collections.emptySet() : stored
        );
    }

    public void save(ProtectedItemProfile profile)
    {
        if (profile == null)
        {
            return;
        }

        configManager.setRSProfileConfiguration(
                GROUP,
                KEY,
                gson.toJson(profile.snapshot())
        );
    }
}
