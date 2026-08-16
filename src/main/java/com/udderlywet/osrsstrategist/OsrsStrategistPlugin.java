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
import net.runelite.api.events.GameTick;
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
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "OSRS Strategist",
        description = "Adaptive progression strategy for every OSRS account type",
        tags = {"strategy", "progression", "maxing", "ironman", "uim", "gim", "clues", "farming"}
)
public class OsrsStrategistPlugin extends Plugin
{
    private static final double COMPLETION_VARIETY_PENALTY = -10.0;
    private static final long COMPLETION_VARIETY_DURATION_MILLIS = 30L * 60L * 1000L;

    @Inject private OsrsStrategistConfig config;
    @Inject private ClientToolbar clientToolbar;
    @Inject private OverlayManager overlayManager;
    @Inject private StrategyDataAssembler strategyDataAssembler;
    @Inject private StrategyEngine strategyEngine;
    @Inject private MethodGuidanceService methodGuidanceService;
    @Inject private AccountPreferenceStore accountPreferenceStore;
    @Inject private AccountStrategyProfileStore accountStrategyProfileStore;
    @Inject private AccountMilestoneStore accountMilestoneStore;
    @Inject private AccountRecommendationHistoryStore accountRecommendationHistoryStore;
    @Inject private MilestoneTracker milestoneTracker;
    @Inject private SkillIconLoader skillIconLoader;
    @Inject private AccessObservationService accessObservationService;
    @Inject private FarmingRunObservationService farmingRunObservationService;
    @Inject private MilestoneRewardOverlay milestoneRewardOverlay;
    @Inject private MethodGuidanceOverlay methodGuidanceOverlay;

    private final PreferenceProfile preferenceProfile = new PreferenceProfile();
    private final RecommendationHistory recommendationHistory = new RecommendationHistory();
    private String loadedPreferenceProfileKey;
    private String loadedStrategyProfileKey;
    private String loadedMilestoneProfileKey;
    private String loadedHistoryProfileKey;
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
        panel = new OsrsStrategistPanel(this::applyRecommendationFeedback, skillIconLoader);
        navButton = NavigationButton.builder()
                .tooltip("OSRS Strategist")
                .icon(createTemporaryIcon())
                .priority(5)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
        overlayManager.add(milestoneRewardOverlay);
        overlayManager.add(methodGuidanceOverlay);
        syncPreferenceProfile();
        syncStrategyProfile();
        syncMilestoneProfile();
        syncRecommendationHistory();
        updateAccountPanel();
    }

    @Override
    protected void shutDown()
    {
        if (navButton != null) clientToolbar.removeNavigation(navButton);
        overlayManager.remove(milestoneRewardOverlay);
        overlayManager.remove(methodGuidanceOverlay);
        milestoneRewardOverlay.clear();
        methodGuidanceOverlay.clear();
        preferenceProfile.clear();
        recommendationHistory.clear();
        strategyDataAssembler.clearForAccountChange();
        accessObservationService.clearForAccountChange();
        farmingRunObservationService.clearForAccountChange();
        loadedPreferenceProfileKey = null;
        loadedStrategyProfileKey = null;
        loadedMilestoneProfileKey = null;
        loadedHistoryProfileKey = null;
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
    public void onGameTick(GameTick event)
    {
        boolean accessChanged = accessObservationService.observeCurrentLocation();
        boolean farmChanged = farmingRunObservationService.observeCurrentPatches();
        if (accessChanged || farmChanged) updateAccountPanel();
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        updateAccountPanel();
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        updateAccountPanel();
    }

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    {
        if (savingProfileConfiguration) return;
        loadedPreferenceProfileKey = null;
        loadedStrategyProfileKey = null;
        loadedMilestoneProfileKey = null;
        loadedHistoryProfileKey = null;
        trackedMilestone = null;
        recommendationHistory.clear();
        strategyDataAssembler.clearForAccountChange();
        accessObservationService.clearForAccountChange();
        farmingRunObservationService.clearForAccountChange();
        methodGuidanceOverlay.clear();
        syncPreferenceProfile();
        syncStrategyProfile();
        syncMilestoneProfile();
        syncRecommendationHistory();
        updateAccountPanel();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!OsrsStrategistConfig.GROUP.equals(event.getGroup())) return;
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
            loadedStrategyProfileKey = accountStrategyProfileStore.getActiveProfileKey();
        }
        updateAccountPanel();
    }

    void applyRecommendationFeedback(String activityId, FeedbackAction action)
    {
        if (activityId == null || action == null) return;
        syncPreferenceProfile();
        syncRecommendationHistory();
        preferenceProfile.apply(activityId, action);

        RecommendationHistoryAction historyAction = historyAction(action);
        if (historyAction != null)
        {
            recommendationHistory.add(activityId, null, historyAction);
            saveRecommendationHistory();
        }

        refreshStrategyImmediately();
        savePreferenceProfile();
    }

    private void refreshStrategyImmediately()
    {
        if (panel == null || latestData == null) return;
        PlayerStrategyProfile profile = effectiveStrategyProfile();
        StrategyResult result = evaluate(latestData, profile);
        updateTrackedMilestone(
                result.getRecommendations(),
                latestData.getCollectionLog()
        );
        updateGuidance(result, latestData);

        Runnable update = () ->
        {
            panel.updateRecommendations(result.getRecommendations());
            panel.updateOpportunities(result.getOpportunities());
            panel.revalidate();
            panel.repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) update.run();
        else SwingUtilities.invokeLater(update);
    }

    private StrategyResult evaluate(StrategyDataBundle data, PlayerStrategyProfile profile)
    {
        return strategyEngine.evaluate(
                data,
                profile.getStrategyMode(),
                profile.getSessionIntent(),
                profile.getQuestTolerance(),
                profile.getActiveGoal(),
                profile.isUseGroupStorage(),
                profile.isCollectionistMode(),
                profile.isAllowWildernessMethods(),
                preferenceProfile,
                recommendationHistory,
                profile.getVarietyPreference());
    }

    private void updateGuidance(StrategyResult result, StrategyDataBundle data)
    {
        if (!config.showInGameGuidance()
                || result == null
                || result.getRecommendations().isEmpty())
        {
            methodGuidanceOverlay.clear();
            return;
        }
        GuidanceChecklist checklist = methodGuidanceService.build(
                result.getRecommendations().get(0), data);
        methodGuidanceOverlay.update(checklist);
    }

    private void syncPreferenceProfile()
    {
        String activeKey = accountPreferenceStore.getActiveProfileKey();
        if (Objects.equals(loadedPreferenceProfileKey, activeKey) && activeKey != null) return;
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
                && activeKey != null && strategyProfile != null) return;
        PlayerStrategyProfile defaults = PlayerStrategyProfile.fromConfig(config);
        strategyProfile = accountStrategyProfileStore.loadOrDefault(defaults);
        loadedStrategyProfileKey = activeKey;
    }

    private void syncMilestoneProfile()
    {
        String activeKey = accountMilestoneStore.getActiveProfileKey();
        if (Objects.equals(loadedMilestoneProfileKey, activeKey) && activeKey != null) return;
        trackedMilestone = activeKey == null ? null : accountMilestoneStore.load();
        loadedMilestoneProfileKey = activeKey;
    }

    private void syncRecommendationHistory()
    {
        String activeKey = accountRecommendationHistoryStore.getActiveProfileKey();
        if (Objects.equals(loadedHistoryProfileKey, activeKey) && activeKey != null) return;
        recommendationHistory.clear();
        if (activeKey == null)
        {
            loadedHistoryProfileKey = null;
            return;
        }
        accountRecommendationHistoryStore.loadInto(recommendationHistory);
        loadedHistoryProfileKey = activeKey;
    }

    private PlayerStrategyProfile effectiveStrategyProfile()
    {
        if (strategyProfile == null) strategyProfile = PlayerStrategyProfile.fromConfig(config);
        return strategyProfile;
    }

    private void updateAccountPanel()
    {
        if (panel == null) return;
        StrategyDataBundle data = strategyDataAssembler.read();
        if (data == null || data.getAccount() == null)
        {
            latestData = null;
            methodGuidanceOverlay.clear();
            PlayerStrategyProfile profile = effectiveStrategyProfile();
            SwingUtilities.invokeLater(() ->
            {
                panel.updateAccount("Waiting for login...", "Unknown", 0);
                panel.updateGoal(profile.getActiveGoal());
                panel.updateStrategy(profile.getStrategyMode(),
                        profile.getSessionIntent(), profile.getQuestTolerance());
                panel.updateRecommendations(Collections.emptyList());
                panel.updateOpportunities(Collections.emptyList());
            });
            return;
        }

        latestData = data;
        syncPreferenceProfile();
        syncStrategyProfile();
        syncMilestoneProfile();
        syncRecommendationHistory();

        TrackedMilestone completedCheckpoint = trackedMilestone;
        MilestoneCompletion completion = milestoneTracker.detectCompletion(
                completedCheckpoint, data.getAccount());
        if (completion != null)
        {
            if (completedCheckpoint == null
                    || !completedCheckpoint.isProgressionProtected())
            {
                preferenceProfile.addTemporaryScoreAdjustment(
                        completion.getActivityId(), COMPLETION_VARIETY_PENALTY,
                        COMPLETION_VARIETY_DURATION_MILLIS);
                savePreferenceProfile();
            }

            recommendationHistory.add(
                    completion.getActivityId(),
                    completion.getTitle(),
                    RecommendationHistoryAction.COMPLETED
            );
            saveRecommendationHistory();

            trackedMilestone = null;
            saveTrackedMilestone();
            milestoneRewardOverlay.show(completion);
        }

        PlayerStrategyProfile profile = effectiveStrategyProfile();
        StrategyResult result = evaluate(data, profile);
        updateTrackedMilestone(
                result.getRecommendations(),
                data.getCollectionLog()
        );
        updateGuidance(result, data);
        AccountSnapshot account = data.getAccount();

        SwingUtilities.invokeLater(() ->
        {
            panel.updateAccount(account.getPlayerName(), account.getAccountTypeName(),
                    account.getMembershipStatus().getDisplayName(), account.getTotalLevel());
            panel.updateGoal(profile.getActiveGoal());
            panel.updateStrategy(profile.getStrategyMode(), profile.getSessionIntent(),
                    profile.getQuestTolerance());
            panel.updateRecommendations(result.getRecommendations());
            panel.updateOpportunities(result.getOpportunities());
        });
    }

    private void updateTrackedMilestone(
            List<Recommendation> recommendations,
            CollectionLogSnapshot collectionLog)
    {
        TrackedMilestone candidate = milestoneTracker.fromRecommendations(
                recommendations,
                collectionLog
        );
        if (milestoneTracker.sameCheckpoint(trackedMilestone, candidate)) return;
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
        loadedPreferenceProfileKey = accountPreferenceStore.getActiveProfileKey();
    }

    private void saveRecommendationHistory()
    {
        savingProfileConfiguration = true;
        try
        {
            accountRecommendationHistoryStore.save(recommendationHistory);
        }
        finally
        {
            savingProfileConfiguration = false;
        }
        loadedHistoryProfileKey = accountRecommendationHistoryStore.getActiveProfileKey();
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
        loadedMilestoneProfileKey = accountMilestoneStore.getActiveProfileKey();
    }

    private static RecommendationHistoryAction historyAction(FeedbackAction action)
    {
        switch (action)
        {
            case LATER:
                return RecommendationHistoryAction.LATER;
            case NOT_TODAY:
                return RecommendationHistoryAction.NOT_TODAY;
            case DISLIKE:
                return RecommendationHistoryAction.DISLIKE;
            case DO_THIS:
            default:
                return null;
        }
    }

    private BufferedImage createTemporaryIcon()
    {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
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
