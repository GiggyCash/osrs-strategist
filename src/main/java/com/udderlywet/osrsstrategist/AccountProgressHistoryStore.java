package com.udderlywet.osrsstrategist;

import com.google.gson.Gson;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/** Corrupt-safe progress persistence scoped by RuneLite's character profile. */
@Singleton
public class AccountProgressHistoryStore
{
    static final String GROUP = "osrs-strategist-profile";
    static final String KEY = "progress-history";

    interface ProfileConfiguration
    {
        String activeProfileKey();
        String get(String group, String key);
        void set(String group, String key, String value);
        void unset(String group, String key);
    }

    private final ProfileConfiguration configuration;
    private final Gson gson;

    @Inject
    public AccountProgressHistoryStore(ConfigManager configManager, Gson gson)
    {
        this(new RuneLiteProfileConfiguration(configManager), gson);
    }

    AccountProgressHistoryStore(ProfileConfiguration configuration, Gson gson)
    {
        this.configuration = configuration;
        this.gson = gson;
    }

    public String getActiveProfileKey()
    {
        return configuration.activeProfileKey();
    }

    public ProgressHistory load()
    {
        if (getActiveProfileKey() == null) return new ProgressHistory();
        return ProgressHistoryCodec.decode(gson, configuration.get(GROUP, KEY));
    }

    public void save(ProgressHistory history)
    {
        if (history == null || getActiveProfileKey() == null) return;
        configuration.set(GROUP, KEY,
                ProgressHistoryCodec.encode(gson, history));
    }

    public void clear()
    {
        if (getActiveProfileKey() != null) configuration.unset(GROUP, KEY);
    }

    private static final class RuneLiteProfileConfiguration
            implements ProfileConfiguration
    {
        private final ConfigManager configManager;

        private RuneLiteProfileConfiguration(ConfigManager configManager)
        {
            this.configManager = configManager;
        }

        public String activeProfileKey()
        {
            return configManager.getRSProfileKey();
        }

        public String get(String group, String key)
        {
            return configManager.getRSProfileConfiguration(group, key);
        }

        public void set(String group, String key, String value)
        {
            configManager.setRSProfileConfiguration(group, key, value);
        }

        public void unset(String group, String key)
        {
            configManager.unsetRSProfileConfiguration(group, key);
        }
    }
}
