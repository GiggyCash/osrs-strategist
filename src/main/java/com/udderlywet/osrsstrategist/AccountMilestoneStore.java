package com.udderlywet.osrsstrategist;

import com.google.gson.Gson;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Keeps the currently watched checkpoint per RuneScape character.
 *
 * <p>This lets Strategist notice a milestone after a client restart instead of
 * forgetting what it had been asking the player to do.</p>
 */
@Singleton
public class AccountMilestoneStore
{
    private static final String GROUP = "osrs-strategist-profile";
    private static final String ACTIVE_MILESTONE_KEY = "activeMilestone";

    private final ConfigManager configManager;
    private final Gson gson;

    @Inject
    public AccountMilestoneStore(
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

    public TrackedMilestone load()
    {
        if (getActiveProfileKey() == null)
        {
            return null;
        }

        String json = configManager.getRSProfileConfiguration(
                GROUP,
                ACTIVE_MILESTONE_KEY
        );

        if (json == null || json.trim().isEmpty())
        {
            return null;
        }

        return gson.fromJson(json, TrackedMilestone.class);
    }

    public void save(TrackedMilestone milestone)
    {
        if (milestone == null)
        {
            configManager.unsetRSProfileConfiguration(
                    GROUP,
                    ACTIVE_MILESTONE_KEY
            );
            return;
        }

        // RuneLite can create the profile on first write when necessary.
        configManager.setRSProfileConfiguration(
                GROUP,
                ACTIVE_MILESTONE_KEY,
                gson.toJson(milestone)
        );
    }
}
