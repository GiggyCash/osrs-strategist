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
 * Persists learned Strategist preferences, explicit cooldowns, and temporary
 * soft score adjustments in RuneLite's RuneScape-profile configuration.
 */
@Singleton
public class AccountPreferenceStore
{
    private static final String GROUP = "osrs-strategist-profile";
    private static final String PREFERENCES_KEY = "preferences";
    private static final String COOLDOWNS_KEY = "cooldowns";
    private static final String TIMED_ADJUSTMENTS_KEY = "timedAdjustments";

    private static final Type PREFERENCE_MAP_TYPE =
            new TypeToken<Map<String, Double>>() { }.getType();

    private static final Type COOLDOWN_MAP_TYPE =
            new TypeToken<Map<String, Long>>() { }.getType();

    private static final Type TIMED_ADJUSTMENT_MAP_TYPE =
            new TypeToken<Map<String, TimedScoreAdjustment>>() { }.getType();

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

        String preferenceJson =
                configManager.getRSProfileConfiguration(
                        GROUP,
                        PREFERENCES_KEY
                );

        if (preferenceJson != null
                && !preferenceJson.trim().isEmpty())
        {
            Map<String, Double> storedPreferences =
                    gson.fromJson(
                            preferenceJson,
                            PREFERENCE_MAP_TYPE
                    );

            if (storedPreferences == null)
            {
                storedPreferences = Collections.emptyMap();
            }

            profile.replaceAll(storedPreferences);
        }

        String cooldownJson =
                configManager.getRSProfileConfiguration(
                        GROUP,
                        COOLDOWNS_KEY
                );

        if (cooldownJson != null
                && !cooldownJson.trim().isEmpty())
        {
            Map<String, Long> storedCooldowns =
                    gson.fromJson(
                            cooldownJson,
                            COOLDOWN_MAP_TYPE
                    );

            if (storedCooldowns == null)
            {
                storedCooldowns = Collections.emptyMap();
            }

            profile.replaceCooldowns(storedCooldowns);
        }

        String adjustmentJson =
                configManager.getRSProfileConfiguration(
                        GROUP,
                        TIMED_ADJUSTMENTS_KEY
                );

        if (adjustmentJson != null
                && !adjustmentJson.trim().isEmpty())
        {
            Map<String, TimedScoreAdjustment> storedAdjustments =
                    gson.fromJson(
                            adjustmentJson,
                            TIMED_ADJUSTMENT_MAP_TYPE
                    );

            if (storedAdjustments == null)
            {
                storedAdjustments = Collections.emptyMap();
            }

            profile.replaceTimedAdjustments(storedAdjustments);
        }
    }

    public void save(PreferenceProfile profile)
    {
        // RuneLite creates the per-character RS profile on the first write
        // when necessary, so do not require an existing profile key here.
        configManager.setRSProfileConfiguration(
                GROUP,
                PREFERENCES_KEY,
                gson.toJson(profile.snapshot())
        );

        configManager.setRSProfileConfiguration(
                GROUP,
                COOLDOWNS_KEY,
                gson.toJson(profile.cooldownSnapshot())
        );

        configManager.setRSProfileConfiguration(
                GROUP,
                TIMED_ADJUSTMENTS_KEY,
                gson.toJson(profile.timedAdjustmentSnapshot())
        );
    }
}
