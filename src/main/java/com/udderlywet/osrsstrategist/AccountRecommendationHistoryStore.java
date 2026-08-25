package com.udderlywet.osrsstrategist;

import com.google.gson.Gson;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/** Per-RuneScape-profile bounded recommendation history. */
@Singleton
public class AccountRecommendationHistoryStore
{
    static final String GROUP = "osrs-strategist-profile";
    private static final String KEY = "recommendation-history";

    private final ConfigManager configManager;
    private final Gson gson;

    @Inject
    public AccountRecommendationHistoryStore(
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

    public void loadInto(RecommendationHistory history)
    {
        if (history == null) return;
        history.clear();
        if (getActiveProfileKey() == null) return;

        String json = configManager.getRSProfileConfiguration(GROUP, KEY);
        if (json == null || json.trim().isEmpty()) return;

        RecommendationHistoryDocument document = gson.fromJson(
                json, RecommendationHistoryDocument.class);
        if (document != null)
        {
            history.replaceAll(document.getEntries());
        }
    }

    public void save(RecommendationHistory history)
    {
        if (history == null) return;
        configManager.setRSProfileConfiguration(
                GROUP,
                KEY,
                gson.toJson(new RecommendationHistoryDocument(history.snapshot()))
        );
    }
}
