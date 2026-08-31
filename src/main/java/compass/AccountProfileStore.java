package compass;

import com.google.gson.Gson;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/** Central RuneLite character-profile persistence boundary. */
@Singleton
public class AccountProfileStore
{
    static final String GROUP = "osrs-strategist-profile";
    private static final String PREFERENCES = "preferences";
    private static final String COOLDOWNS = "cooldowns";
    private static final String ADJUSTMENTS = "timedAdjustments";
    private static final String STRATEGY = "strategy-settings";
    private static final String MILESTONE = "activeMilestone";
    private static final String RECOMMENDATIONS = "recommendation-history";
    private static final String PROGRESS = "progress-history";

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
    public AccountProfileStore(ConfigManager configManager, Gson gson)
    {
        this(new RuneLiteProfileConfiguration(configManager), gson);
    }

    AccountProfileStore(ProfileConfiguration configuration, Gson gson)
    {
        this.configuration = configuration;
        this.gson = gson;
    }

    public String activeProfileKey() { return configuration.activeProfileKey(); }

    public void loadPreferences(PreferenceProfile profile)
    {
        profile.clear();
        if (activeProfileKey() == null) return;
        var preferences = ProfileJsonCodec.doubles(gson, get(PREFERENCES));
        var cooldowns = ProfileJsonCodec.longs(gson, get(COOLDOWNS));
        Map<String, TimedScoreAdjustment> adjustments =
                ProfileJsonCodec.timedAdjustments(gson, get(ADJUSTMENTS));
        profile.replaceAll(preferences == null ? Collections.emptyMap() : preferences);
        profile.replaceCooldowns(cooldowns == null ? Collections.emptyMap() : cooldowns);
        profile.replaceTimedAdjustments(adjustments == null
                ? Collections.emptyMap() : adjustments);
    }

    public void savePreferences(PreferenceProfile profile)
    {
        set(PREFERENCES, profile.snapshot());
        set(COOLDOWNS, profile.cooldownSnapshot());
        set(ADJUSTMENTS, profile.timedAdjustmentSnapshot());
    }

    public void clearPreferences()
    {
        unset(PREFERENCES);
        unset(COOLDOWNS);
        unset(ADJUSTMENTS);
    }

    public PlayerStrategyProfile loadStrategy(PlayerStrategyProfile defaults)
    {
        if (activeProfileKey() == null) return defaults;
        var stored = fromJson(STRATEGY, PlayerStrategyProfile.class);
        return stored == null ? defaults : stored.sanitizedForPublicProduct();
    }

    public void saveStrategy(PlayerStrategyProfile profile)
    {
        if (profile != null) set(STRATEGY, profile);
    }

    public TrackedMilestone loadMilestone()
    {
        return activeProfileKey() == null ? null
                : fromJson(MILESTONE, TrackedMilestone.class);
    }

    public void saveMilestone(TrackedMilestone milestone)
    {
        if (milestone == null) unset(MILESTONE);
        else set(MILESTONE, milestone);
    }

    public void loadRecommendations(RecommendationHistory history)
    {
        if (history == null) return;
        history.clear();
        if (activeProfileKey() == null) return;
        RecommendationHistoryDocument stored = fromJson(
                RECOMMENDATIONS, RecommendationHistoryDocument.class);
        if (stored != null) history.replaceAll(stored.getEntries());
    }

    public void saveRecommendations(RecommendationHistory history)
    {
        if (history != null) set(RECOMMENDATIONS,
                new RecommendationHistoryDocument(history.snapshot()));
    }

    public ProgressHistory loadProgress()
    {
        return activeProfileKey() == null ? new ProgressHistory()
                : ProgressHistoryCodec.decode(gson, get(PROGRESS));
    }

    public void saveProgress(ProgressHistory history)
    {
        if (history != null && activeProfileKey() != null)
            configuration.set(GROUP, PROGRESS,
                    ProgressHistoryCodec.encode(gson, history));
    }

    public void clearProgress() { unset(PROGRESS); }

    private String get(String key)
    {
        var json = configuration.get(GROUP, key);
        return json == null || json.trim().isEmpty() ? null : json;
    }

    private <T> T fromJson(String key, Class<T> type)
    {
        var json = get(key);
        return json == null ? null : gson.fromJson(json, type);
    }

    private void set(String key, Object value)
    {
        configuration.set(GROUP, key, gson.toJson(value));
    }

    private void unset(String key)
    {
        if (activeProfileKey() != null) configuration.unset(GROUP, key);
    }

    private static final class RuneLiteProfileConfiguration
            implements ProfileConfiguration
    {
        private final ConfigManager config;
        private RuneLiteProfileConfiguration(ConfigManager config) { this.config = config; }
        public String activeProfileKey() { return config.getRSProfileKey(); }
        public String get(String group, String key)
        { return config.getRSProfileConfiguration(group, key); }
        public void set(String group, String key, String value)
        { config.setRSProfileConfiguration(group, key, value); }
        public void unset(String group, String key)
        { config.unsetRSProfileConfiguration(group, key); }
    }
}
