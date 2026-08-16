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
import net.runelite.api.events.ItemContainerChanged;
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
    /**
     * Finishing a suggested checkpoint should usually produce something fresh,
     * but should not hard-block a strategically valuable continuation.
     */
    private static final double COMPLETION_VARIETY_PENALTY = -10.0;
    private static final long COMPLETION_VARIETY_DURATION_MILLIS =
            30L * 60L * 1000L;

    @Inject private OsrsStrategistConfig config;
    @Inject private ClientToolbar clientToolbar;
    @Inject private StrategyDataAssembler strategyDataAssembler;
    @Inject private StrategyEngine strategyEngine;
    @Inject private AccountPreferenceStore accountPreferenceStore;
    @Inject private AccountStrategyProfileStore accountStrategyProfileStore;
    @Inject private AccountMilestoneStore accountMilestoneStore;
    @Inject private MilestoneTracker milestoneTracker;
    @Inject private SkillIconLoader skillIconLoader;

    /** Learned likes/dislikes and recommendation cooldowns for this character. */
    private final PreferenceProfile preferenceProfile = new PreferenceProfile();

    private String loadedPreferenceProfileKey;
    private String loadedStrategyProfileKey;
    private String loadedMilestoneProfileKey;
    private boolean savingProfileConfiguration;
    private PlayerStrategyProfile strategyProfile;
    private TrackedMilestone trackedMilestone;
    private StrategyDataBundle latestData;
    private NavigationButton navButton;
    private OsrsStrategistPanel panel;

    @Provides
    OsrsStrategistConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(OsrsStrategistConfig.class);
    }

    @Override
    protected void startUp()
    {
        panel = new OsrsStrategistPanel(
                this::applyRecommendationFeedback,
                skillIconLoader
        );

        navButton = NavigationButton.builder()
                .tooltip("OSRS Strategist")
                .icon(createTemporaryIcon())
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        syncPreferenceProfile();
        syncStrategyProfile();
        syncMilestoneProfile();
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
        strategyDataAssembler.clearForAccountChange();
        loadedPreferenceProfileKey = null;
        loadedStrategyProfileKey = null;
        loadedMilestoneProfileKey = null;
        savingProfileConfiguration = false;
        strategyProfile = null;
        trackedMilestone = null;
        latestData = null;
        panel = null;
        navButton = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        updateAccountPanel();
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        // This is what makes button-free progression adaptive. A level change
        // rereads the account, detects milestone completion, and reranks now.
        updateAccountPanel();
    }

    /**
     * Inventory/equipment/bank changes can alter the best training method even
     * when no skill level changes, so refresh the strategy when containers move.
     */
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        updateAccountPanel();
    }

    @Subscribe
    public void onRuneScapeProfileChanged(
            RuneScapeProfileChanged event)
    {
        // The first write to RuneLite profile config can itself create a profile
        // and fire this event. Ignore that internal transition while saving.
        if (savingProfileConfiguration)
        {
            return;
        }

        loadedPreferenceProfileKey = null;
        loadedStrategyProfileKey = null;
        loadedMilestoneProfileKey = null;
        trackedMilestone = null;
        strategyDataAssembler.clearForAccountChange();

        syncPreferenceProfile();
        syncStrategyProfile();
        syncMilestoneProfile();
        updateAccountPanel();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!OsrsStrategistConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }

        // A settings change while logged in becomes this character's explicit
        // strategy profile instead of silently affecting every account forever.
        strategyProfile = PlayerStrategyProfile.fromConfig(config);

        if (accountStrategyProfileStore.getActiveProfileKey() != null)
        {
            savingProfileConfiguration = true;
            try
            {
                accountStrategyProfileStore.save(strategyProfile);
            }
            finally
            {
                savingProfileConfiguration = false;
            }
            loadedStrategyProfileKey =
                    accountStrategyProfileStore.getActiveProfileKey();
        }

        updateAccountPanel();
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

        // Rotate the recommendation before persisting so button feedback feels
        // immediate even if profile storage takes a moment.
        refreshStrategyImmediately();
        savePreferenceProfile();
    }

    private void refreshStrategyImmediately()
    {
        if (panel == null || latestData == null)
        {
            return;
        }

        PlayerStrategyProfile profile = effectiveStrategyProfile();
        StrategyResult result = evaluate(latestData, profile);
        updateTrackedMilestone(result.getRecommendations());

        Runnable update = () ->
        {
            panel.updateRecommendations(result.getRecommendations());
            panel.updateOpportunities(result.getOpportunities());
            panel.revalidate();
            panel.repaint();
        };

        if (SwingUtilities.isEventDispatchThread())
        {
            update.run();
        }
        else
        {
            SwingUtilities.invokeLater(update);
        }
    }

    private StrategyResult evaluate(
            StrategyDataBundle data,
            PlayerStrategyProfile profile)
    {
        return strategyEngine.evaluate(
                data,
                profile.getStrategyMode(),
                profile.getSessionIntent(),
                profile.getQuestTolerance(),
                profile.getActiveGoal(),
                profile.isUseGroupStorage(),
                profile.isCollectionistMode(),
                preferenceProfile
        );
    }

    private void syncPreferenceProfile()
    {
        String activeKey = accountPreferenceStore.getActiveProfileKey();

        if (Objects.equals(loadedPreferenceProfileKey, activeKey)
                && activeKey != null)
        {
            return;
        }

        preferenceProfile.clear();

        if (activeKey == null)
        {
            loadedPreferenceProfileKey = null;
            return;
        }

        accountPreferenceStore.loadInto(preferenceProfile);
        loadedPreferenceProfileKey = activeKey;
    }

    private void syncStrategyProfile()
    {
        String activeKey = accountStrategyProfileStore.getActiveProfileKey();

        if (Objects.equals(loadedStrategyProfileKey, activeKey)
                && activeKey != null
                && strategyProfile != null)
        {
            return;
        }

        PlayerStrategyProfile defaults =
                PlayerStrategyProfile.fromConfig(config);

        strategyProfile = accountStrategyProfileStore
                .loadOrDefault(defaults);
        loadedStrategyProfileKey = activeKey;
    }

    private void syncMilestoneProfile()
    {
        String activeKey = accountMilestoneStore.getActiveProfileKey();

        if (Objects.equals(loadedMilestoneProfileKey, activeKey)
                && activeKey != null)
        {
            return;
        }

        trackedMilestone = activeKey == null
                ? null
                : accountMilestoneStore.load();
        loadedMilestoneProfileKey = activeKey;
    }

    private PlayerStrategyProfile effectiveStrategyProfile()
    {
        if (strategyProfile == null)
        {
            strategyProfile = PlayerStrategyProfile.fromConfig(config);
        }
        return strategyProfile;
    }

    private void updateAccountPanel()
    {
        if (panel == null)
        {
            return;
        }

        StrategyDataBundle data = strategyDataAssembler.read();

        if (data == null || data.getAccount() == null)
        {
            latestData = null;
            PlayerStrategyProfile profile = effectiveStrategyProfile();

            SwingUtilities.invokeLater(() ->
            {
                panel.updateAccount(
                        "Waiting for login...",
                        "Unknown",
                        0
                );
                panel.updateGoal(profile.getActiveGoal());
                panel.updateStrategy(
                        profile.getStrategyMode(),
                        profile.getSessionIntent(),
                        profile.getQuestTolerance()
                );
                panel.updateRecommendations(Collections.emptyList());
                panel.updateOpportunities(Collections.emptyList());
            });
            return;
        }

        latestData = data;
        syncPreferenceProfile();
        syncStrategyProfile();
        syncMilestoneProfile();

        MilestoneCompletion completion = milestoneTracker.detectCompletion(
                trackedMilestone,
                data.getAccount()
        );

        if (completion != null)
        {
            // Encourage a new task without forbidding continuation of the same
            // skill when it remains the best strategic move.
            preferenceProfile.addTemporaryScoreAdjustment(
                    completion.getActivityId(),
                    COMPLETION_VARIETY_PENALTY,
                    COMPLETION_VARIETY_DURATION_MILLIS
            );
            savePreferenceProfile();

            trackedMilestone = null;
            saveTrackedMilestone();
        }

        PlayerStrategyProfile profile = effectiveStrategyProfile();
        StrategyResult result = evaluate(data, profile);
        updateTrackedMilestone(result.getRecommendations());

        AccountSnapshot account = data.getAccount();
        MilestoneCompletion completionForUi = completion;

        SwingUtilities.invokeLater(() ->
        {
            panel.updateAccount(
                    account.getPlayerName(),
                    account.getAccountTypeName(),
                    account.getMembershipStatus().getDisplayName(),
                    account.getTotalLevel()
            );
            panel.updateGoal(profile.getActiveGoal());
            panel.updateStrategy(
                    profile.getStrategyMode(),
                    profile.getSessionIntent(),
                    profile.getQuestTolerance()
            );
            panel.updateRecommendations(result.getRecommendations());
            panel.updateOpportunities(result.getOpportunities());

            if (completionForUi != null)
            {
                panel.showMilestoneCompletion(completionForUi);
            }
        });
    }

    private void updateTrackedMilestone(
            List<Recommendation> recommendations)
    {
        TrackedMilestone candidate =
                milestoneTracker.fromRecommendations(recommendations);

        if (milestoneTracker.sameCheckpoint(trackedMilestone, candidate))
        {
            return;
        }

        trackedMilestone = candidate;
        saveTrackedMilestone();
    }

    private void savePreferenceProfile()
    {
        savingProfileConfiguration = true;
        try
        {
            accountPreferenceStore.save(preferenceProfile);
        }
        finally
        {
            savingProfileConfiguration = false;
        }

        loadedPreferenceProfileKey =
                accountPreferenceStore.getActiveProfileKey();
    }

    private void saveTrackedMilestone()
    {
        savingProfileConfiguration = true;
        try
        {
            accountMilestoneStore.save(trackedMilestone);
        }
        finally
        {
            savingProfileConfiguration = false;
        }

        loadedMilestoneProfileKey =
                accountMilestoneStore.getActiveProfileKey();
    }

    private BufferedImage createTemporaryIcon()
    {
        BufferedImage image = new BufferedImage(
                16,
                16,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(60, 45, 30));
        graphics.fillRect(0, 0, 16, 16);
        graphics.setColor(new Color(212, 167, 44));
        graphics.drawOval(1, 1, 13, 13);
        graphics.drawLine(8, 3, 8, 13);
        graphics.drawLine(3, 8, 13, 8);
        graphics.dispose();

        return image;
    }
}
