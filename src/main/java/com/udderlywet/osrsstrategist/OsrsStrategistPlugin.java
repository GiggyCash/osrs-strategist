package com.udderlywet.osrsstrategist;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
        name = "OSRS Strategist",
        description = "Adaptive progression strategy for every OSRS account type",
        tags = {
                "strategy",
                "progression",
                "maxing",
                "ironman",
                "uim",
                "gim",
                "clues",
                "farming"
        }
)
public class OsrsStrategistPlugin extends Plugin
{
    @Inject
    private OsrsStrategistConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private AccountReader accountReader;

    @Inject
    private RecommendationEngine recommendationEngine;

    @Inject
    private AccountPreferenceStore accountPreferenceStore;

    private final PreferenceProfile preferenceProfile =
            new PreferenceProfile();

    private String loadedPreferenceProfileKey;
    private boolean savingPreferenceProfile;
    private NavigationButton navButton;
    private OsrsStrategistPanel panel;

    @Provides
    OsrsStrategistConfig provideConfig(
            ConfigManager manager)
    {
        return manager.getConfig(
                OsrsStrategistConfig.class
        );
    }

    @Override
    protected void startUp()
    {
        panel = new OsrsStrategistPanel(
                this::applyRecommendationFeedback
        );

        BufferedImage icon =
                createTemporaryIcon();

        navButton = NavigationButton.builder()
                .tooltip("OSRS Strategist")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        syncPreferenceProfile();
        updateAccountPanel();
    }

    @Override
    protected void shutDown()
    {
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
        }

        preferenceProfile.clear();
        loadedPreferenceProfileKey = null;
        savingPreferenceProfile = false;
        panel = null;
        navButton = null;
    }

    @Subscribe
    public void onGameStateChanged(
            GameStateChanged event)
    {
        updateAccountPanel();
    }

    @Subscribe
    public void onStatChanged(
            StatChanged event)
    {
        updateAccountPanel();
    }

    @Subscribe
    public void onRuneScapeProfileChanged(
            RuneScapeProfileChanged event)
    {
        loadedPreferenceProfileKey = null;

        // setRSProfileConfiguration can create a profile and post this event
        // while a feedback save is still in progress. Do not reload stale data
        // until that save has completed.
        if (savingPreferenceProfile)
        {
            return;
        }

        syncPreferenceProfile();
        updateAccountPanel();
    }

    @Subscribe
    public void onConfigChanged(
            ConfigChanged event)
    {
        if (OsrsStrategistConfig.GROUP.equals(
                event.getGroup()))
        {
            updateAccountPanel();
        }
    }

    void applyRecommendationFeedback(
            String activityId,
            FeedbackAction action)
    {
        if (activityId == null || action == null)
        {
            return;
        }

        syncPreferenceProfile();
        preferenceProfile.apply(activityId, action);

        savingPreferenceProfile = true;

        try
        {
            accountPreferenceStore.save(
                    preferenceProfile
            );
        }
        finally
        {
            savingPreferenceProfile = false;
        }

        // A first-time save may have created the RuneScape profile.
        // Reload after the write so the recommendation engine sees the
        // newly persisted value immediately.
        loadedPreferenceProfileKey = null;
        syncPreferenceProfile();
        updateAccountPanel();
    }

    private void syncPreferenceProfile()
    {
        String activeProfileKey =
                accountPreferenceStore.getActiveProfileKey();

        if (Objects.equals(
                loadedPreferenceProfileKey,
                activeProfileKey)
                && activeProfileKey != null)
        {
            return;
        }

        preferenceProfile.clear();

        if (activeProfileKey == null)
        {
            loadedPreferenceProfileKey = null;
            return;
        }

        accountPreferenceStore.loadInto(
                preferenceProfile
        );

        loadedPreferenceProfileKey =
                activeProfileKey;
    }

    private void updateAccountPanel()
    {
        if (panel == null)
        {
            return;
        }

        AccountSnapshot snapshot =
                accountReader.read();

        if (snapshot == null)
        {
            SwingUtilities.invokeLater(
                    () ->
                    {
                        panel.updateAccount(
                                "Waiting for login...",
                                "Unknown",
                                0
                        );

                        panel.updateStrategy(
                                config.strategyMode(),
                                config.questTolerance()
                        );

                        panel.updateRecommendations(
                                Collections.emptyList()
                        );
                    }
            );

            return;
        }

        syncPreferenceProfile();

        List<Recommendation> recommendations =
                recommendationEngine.recommend(
                        snapshot,
                        config.strategyMode(),
                        preferenceProfile
                );

        SwingUtilities.invokeLater(
                () ->
                {
                    panel.updateAccount(
                            snapshot.getPlayerName(),
                            snapshot.getAccountTypeName(),
                            snapshot.getTotalLevel()
                    );

                    panel.updateStrategy(
                            config.strategyMode(),
                            config.questTolerance()
                    );

                    panel.updateRecommendations(
                            recommendations
                    );
                }
        );
    }

    private BufferedImage createTemporaryIcon()
    {
        BufferedImage image =
                new BufferedImage(
                        16,
                        16,
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D graphics =
                image.createGraphics();

        graphics.setColor(
                new Color(60, 45, 30)
        );

        graphics.fillRect(
                0,
                0,
                16,
                16
        );

        graphics.setColor(
                new Color(212, 167, 44)
        );

        graphics.drawOval(
                1,
                1,
                13,
                13
        );

        graphics.drawLine(
                8,
                3,
                8,
                13
        );

        graphics.drawLine(
                3,
                8,
                13,
                8
        );

        graphics.dispose();

        return image;
    }
}
