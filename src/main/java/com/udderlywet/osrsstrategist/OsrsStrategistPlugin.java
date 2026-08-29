package com.udderlywet.osrsstrategist;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Objects;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.cluescrolls.ClueScrollPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

@PluginDescriptor(
        name = "Gielinor Compass",
        description = "Decides your next safe, useful move from observed account state",
        tags = {"strategy", "progression", "goals", "ironman", "uim", "gim", "clues", "slayer"}
)
@PluginDependency(ClueScrollPlugin.class)
public class OsrsStrategistPlugin extends Plugin
{
    private static final double COMPLETION_VARIETY_PENALTY = -10.0;
    private static final long COMPLETION_VARIETY_DURATION_MILLIS = 30L * 60L * 1000L;
    private static final long PROGRESS_CHECKPOINT_INTERVAL_MILLIS = 60_000L;

    @Inject private OsrsStrategistConfig config;
    @Inject private ConfigManager configManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private OverlayManager overlayManager;
    @Inject private StrategyDataAssembler strategyDataAssembler;
    @Inject private LiveItemStateReader liveItemStateReader;
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
    @Inject private TrainingFatigueTracker trainingFatigueTracker;
    @Inject private MilestoneRewardOverlay milestoneRewardOverlay;
    @Inject private MethodGuidanceOverlay methodGuidanceOverlay;
    @Inject private RecommendationDetailsOverlay recommendationDetailsOverlay;
    @Inject private ProgressAnalyticsService progressAnalyticsService;
    @Inject private AccountProgressHistoryStore accountProgressHistoryStore;
    @Inject private PlanContinuityService planContinuityService;

    private final PreferenceProfile preferenceProfile = new PreferenceProfile();
    private final RecommendationHistory recommendationHistory = new RecommendationHistory();
    private String loadedPreferenceProfileKey;
    private String loadedStrategyProfileKey;
    private String loadedMilestoneProfileKey;
    private String loadedHistoryProfileKey;
    private String loadedProgressProfileKey;
    private boolean savingProfileConfiguration;
    private PlayerStrategyProfile strategyProfile;
    private TrackedMilestone trackedMilestone;
    private StrategyDataBundle latestData;
    private List<Recommendation> latestRecommendations = Collections.emptyList();
    private StrategicPlan latestPlan;
    private ProgressHistory progressHistory = new ProgressHistory();
    private final AccountProgressMilestoneDetector progressMilestoneDetector =
            new AccountProgressMilestoneDetector();
    private NavigationButton navButton;
    private OsrsStrategistPanel panel;
    private final UiGenerationGuard uiGeneration = new UiGenerationGuard();
    private final AtomicBoolean progressUiUpdatePending =
            new AtomicBoolean();
    private boolean varbitRefreshPending;
    private boolean accountRefreshPending;
    private boolean progressCheckpointPending;
    private long lastProgressCheckpointAtMillis;
    private final OverlayLifecycleGuard overlayLifecycle =
            new OverlayLifecycleGuard();
    private final RecommendationStabilizer recommendationStabilizer =
            new RecommendationStabilizer();

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
                skillIconLoader,
                this::updateRecommendationDetails,
                this::updateAccountPanel,
                this::acknowledgeFirstUse,
                SupportLinks.SUPPORT_URL,
                LinkBrowser::browse);
        panel.setDetailsOverlayEnabled(
                OverlayDisplayState.from(config).showsDetails());
        panel.setFirstUseHintVisible(!config.firstUseComplete());
        SidebarAccessibility.apply(panel, config.sidebarTextSize());
        navButton = NavigationButton.builder()
                .tooltip("Gielinor Compass")
                .icon(loadPluginIcon())
                .priority(5)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
        registerOverlays();
        trainingFatigueTracker.clear();
        syncPreferenceProfile();
        syncStrategyProfile();
        syncMilestoneProfile();
        syncRecommendationHistory();
        updateAccountPanel();
    }

    @Override
    protected void shutDown()
    {
        uiGeneration.invalidate();
        if (navButton != null) clientToolbar.removeNavigation(navButton);
        unregisterOverlays();
        milestoneRewardOverlay.clear();
        methodGuidanceOverlay.clear();
        recommendationDetailsOverlay.clear();
        preferenceProfile.clear();
        recommendationHistory.clear();
        archiveProgressSession();
        progressHistory.clear();
        progressAnalyticsService.reset();
        progressMilestoneDetector.clear();
        trainingFatigueTracker.clear();
        strategyDataAssembler.clearForAccountChange();
        accessObservationService.clearForAccountChange();
        farmingRunObservationService.clearForAccountChange();
        loadedPreferenceProfileKey = null;
        loadedStrategyProfileKey = null;
        loadedMilestoneProfileKey = null;
        loadedHistoryProfileKey = null;
        loadedProgressProfileKey = null;
        savingProfileConfiguration = false;
        strategyProfile = null;
        trackedMilestone = null;
        latestData = null;
        latestRecommendations = Collections.emptyList();
        latestPlan = null;
        varbitRefreshPending = false;
        accountRefreshPending = false;
        panel = null;
        navButton = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event == null || event.getGameState() != GameState.LOGGED_IN)
            progressAnalyticsService.pause(System.currentTimeMillis());
        updateAccountPanel();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        boolean accessChanged = accessObservationService.observeCurrentLocation();
        boolean farmChanged = farmingRunObservationService.observeCurrentPatches();
        boolean pohChanged = strategyDataAssembler.observePoh();
        boolean liveStateChanged = consumeVarbitRefreshPending();
        boolean observedStateChanged = consumeAccountRefreshPending();
        checkpointProgressSession();
        if (accessChanged || farmChanged || pohChanged || liveStateChanged
                || observedStateChanged) updateAccountPanel();
    }

    /**
     * Quest, diary, Slayer, and several unlock states are varbit-backed. Coalesce
     * their often-bursty changes into one read on the following game tick.
     */
    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        varbitRefreshPending = true;
    }

    boolean consumeVarbitRefreshPending()
    {
        boolean pending = varbitRefreshPending;
        varbitRefreshPending = false;
        return pending;
    }

    boolean consumeAccountRefreshPending()
    {
        boolean pending = accountRefreshPending;
        accountRefreshPending = false;
        return pending;
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        boolean progressChanged = progressAnalyticsService.record(event);
        if (progressChanged)
        {
            progressCheckpointPending = true;
            updateProgressPanel();
        }
        PlayerStrategyProfile profile = effectiveStrategyProfile();
        TrainingFatigueTracker.FatigueSignal fatigue = trainingFatigueTracker.record(
                event.getSkill(), event.getXp(), profile.getStrategyMode());
        if (fatigue.isPresent())
        {
            preferenceProfile.addTemporaryScoreAdjustment(
                    fatigue.getActivityId(),
                    fatigue.getScoreDelta(),
                    fatigue.getDurationMillis());
        }
        if (fatigue.isPresent() || latestData == null
                || latestData.getAccount() == null
                || latestData.getAccount().getSkillLevel(event.getSkill())
                        != event.getLevel())
        {
            accountRefreshPending = true;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event != null
                && event.getContainerId()
                        == net.runelite.api.gameval.InventoryID.INV_GROUP_TEMP
                && liveItemStateReader != null)
        {
            liveItemStateReader.observeGroupStorage(event.getItemContainer());
        }
        accountRefreshPending = true;
    }

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    {
        if (savingProfileConfiguration) return;
        // Fail closed before reading the new profile. No queued Swing callback,
        // cached bundle, preference weight, or strategy choice from the prior
        // character may survive even for a single refresh generation.
        uiGeneration.invalidate();
        loadedPreferenceProfileKey = null;
        loadedStrategyProfileKey = null;
        loadedMilestoneProfileKey = null;
        loadedHistoryProfileKey = null;
        if (Objects.equals(loadedProgressProfileKey,
                accountProgressHistoryStore.getActiveProfileKey()))
            archiveProgressSession();
        loadedProgressProfileKey = null;
        progressCheckpointPending = false;
        lastProgressCheckpointAtMillis = 0L;
        latestRecommendations = Collections.emptyList();
        latestPlan = null;
        latestData = null;
        preferenceProfile.clear();
        strategyProfile = null;
        trackedMilestone = null;
        recommendationHistory.clear();
        progressHistory = new ProgressHistory();
        progressAnalyticsService.reset();
        progressMilestoneDetector.clear();
        trainingFatigueTracker.clear();
        strategyDataAssembler.clearForAccountChange();
        accessObservationService.clearForAccountChange();
        farmingRunObservationService.clearForAccountChange();
        varbitRefreshPending = false;
        accountRefreshPending = false;
        methodGuidanceOverlay.clear();
        recommendationDetailsOverlay.clear();
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
        String key = event.getKey();
        if (CompassConfigKeys.SIDEBAR_TEXT_SIZE.equals(key))
        {
            if (panel != null)
            {
                SidebarAccessibility.apply(panel, config.sidebarTextSize());
                updateAccountPanel();
            }
            return;
        }
        if (CompassConfigKeys.acknowledgesFirstUse(key)) acknowledgeFirstUse();
        if (CompassConfigKeys.isOverlay(key))
        {
            updateOverlaySettings();
            return;
        }
        if (CompassConfigKeys.FIRST_USE_COMPLETE.equals(key))
        {
            if (panel != null)
                panel.setFirstUseHintVisible(!config.firstUseComplete());
            return;
        }
        if (CompassConfigKeys.RESET_LEARNED_FEEDBACK.equals(key))
        {
            if (!config.resetLearnedFeedback()) return;
            try
            {
                // RuneLite shows ConfigItem.warning before setting this
                // one-shot action to true.
                resetLearnedFeedbackForActiveCharacter();
            }
            finally
            {
                configManager.setConfiguration(
                        OsrsStrategistConfig.GROUP,
                        CompassConfigKeys.RESET_LEARNED_FEEDBACK,
                        false);
            }
            return;
        }
        if (!CompassConfigKeys.changesPlanning(key)) return;

        if (CompassConfigKeys.changesStrategyProfile(key))
        {
            strategyProfile = PlayerStrategyProfile.fromConfig(config);
            saveStrategyProfile();
        }
        // A deliberate strategy-setting change is allowed to replace the plan
        // immediately; stability is for incidental game events only.
        latestRecommendations = Collections.emptyList();
        latestPlan = null;
        if (panel != null) panel.closeDetails();
        recommendationDetailsOverlay.clear();
        updateAccountPanel();
    }

    /** Clear only recommendation learning for the active RuneScape profile. */
    boolean resetLearnedFeedbackForActiveCharacter()
    {
        syncPreferenceProfile();
        String activeKey = accountPreferenceStore.getActiveProfileKey();
        if (activeKey == null) return false;

        savingProfileConfiguration = true;
        try
        {
            accountPreferenceStore.clear();
        }
        finally
        {
            savingProfileConfiguration = false;
        }
        preferenceProfile.clear();
        loadedPreferenceProfileKey = activeKey;

        // Bypass hysteresis for this deliberate recovery action. Completion
        // history, goal/settings, milestone state, and observed account data
        // are intentionally left untouched.
        latestRecommendations = Collections.emptyList();
        accountRefreshPending = true;
        if (recommendationDetailsOverlay != null)
            recommendationDetailsOverlay.clear();
        refreshStrategyImmediately();
        return true;
    }

    void applyRecommendationFeedback(String activityId, FeedbackAction action)
    {
        if (activityId == null || action == null) return;
        syncPreferenceProfile();
        syncRecommendationHistory();
        Recommendation current = null;
        for (Recommendation recommendation : latestRecommendations)
            if (activityId.equals(recommendation.getId()))
            {
                current = recommendation;
                break;
            }
        if (current == null)
            preferenceProfile.apply(activityId, action);
        else
            preferenceProfile.applySemantic(
                    new RecommendationDeduplicator().semanticKey(current),
                    action);

        RecommendationHistoryAction historyAction = historyAction(action);
        if (historyAction != null)
        {
            recommendationHistory.add(activityId, null, historyAction);
            saveRecommendationHistory();
        }

        refreshStrategyImmediately();
        savePreferenceProfile();
    }

    private void updateRecommendationDetails(Recommendation recommendation)
    {
        if (recommendation == null
                || !OverlayDisplayState.from(config).showsDetails())
        {
            recommendationDetailsOverlay.clear();
            refreshMethodGuidanceOverlay();
        }
        else
        {
            AccountSnapshot account = latestData == null
                    ? null : latestData.getAccount();
            recommendationDetailsOverlay.showRecommendation(recommendation,
                    GoalRecommendationContext.assess(
                            effectiveStrategyProfile().getActiveGoal(),
                            recommendation,
                            account == null ? MembershipStatus.UNKNOWN
                                    : account.getMembershipStatus()));
            methodGuidanceOverlay.clear();
        }
    }

    private void refreshStrategyImmediately()
    {
        if (panel == null || latestData == null) return;
        final long generation = uiGeneration.next();
        PlayerStrategyProfile profile = effectiveStrategyProfile();
        StrategyResult result = evaluateAndStabilize(latestData, profile);
        latestRecommendations = new java.util.ArrayList<>(
                result.getRecommendations());
        updateTrackedMilestone(
                result.getRecommendations(),
                latestData.getCollectionLog());
        updateGuidance(result, latestData);
        updateProgressTarget(result);

        Runnable update = () ->
        {
            if (!uiGeneration.isCurrent(generation) || panel == null) return;
            panel.updateRecommendations(result.getRecommendations());
            panel.updateOpportunities(result.getOpportunities());
            panel.updateProgress(progressAnalyticsService.snapshot(),
                    result.getPlan(), progressHistory);
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
                preferenceProfile);
    }

    private StrategyResult evaluateAndStabilize(
            StrategyDataBundle data, PlayerStrategyProfile profile)
    {
        StrategyResult fresh = recommendationStabilizer.stabilize(
                latestRecommendations, evaluate(data, profile));
        StrategyContext context = new StrategyContext(data,
                profile.getStrategyMode(), profile.getSessionIntent(),
                profile.getQuestTolerance(), profile.getActiveGoal(),
                profile.isUseGroupStorage(), profile.isCollectionistMode(),
                profile.isAllowWildernessMethods(), preferenceProfile);
        StrategicPlan previousPlan = latestPlan;
        latestPlan = planContinuityService.reconcile(previousPlan,
                fresh.getPlan(), context, fresh.getRecommendations());
        if (previousPlan != null && previousPlan.matchesContext(context)
                && previousPlan.getCurrentStep().isComplete(data)
                && latestPlan != null
                && !previousPlan.getCurrentStep().getId().equals(
                        latestPlan.getCurrentStep().getId()))
            progressCheckpointPending |= progressAnalyticsService
                    .recordMilestone(new ProgressMilestone(
                            "plan-step:"
                                    + previousPlan.getCurrentStep().getId(),
                            ProgressMilestoneType.PLAN_STEP,
                            previousPlan.getCurrentStep().getObjective()
                                    + " complete",
                            "Completed a proven step on the active goal path.",
                            profile.getActiveGoal().name(),
                            System.currentTimeMillis()));
        return fresh.withPlan(latestPlan);
    }

    private void updateProgressTarget(StrategyResult result)
    {
        if (result == null || result.getRecommendations().isEmpty())
        {
            progressAnalyticsService.clearTarget();
            return;
        }
        Recommendation top = result.getRecommendations().get(0);
        TrainingPlan plan = top.getTrainingPlan();
        if (plan == null || plan.getMethod() == null
                || plan.getMethod().getSkill() == null
                || top.getTargetLevel() < 2)
        {
            progressAnalyticsService.clearTarget();
            return;
        }
        progressAnalyticsService.setTarget(new ProgressTarget(top.getId(),
                plan.getMethod().getId(), plan.getMethod().getSkill(),
                top.getTargetLevel()));
    }

    private void updateProgressPanel()
    {
        if (panel == null || !progressUiUpdatePending.compareAndSet(false, true))
            return;
        Runnable update = () ->
        {
            progressUiUpdatePending.set(false);
            if (panel != null)
                panel.updateProgress(progressAnalyticsService.snapshot(),
                        latestPlan, progressHistory);
        };
        if (SwingUtilities.isEventDispatchThread()) update.run();
        else SwingUtilities.invokeLater(update);
    }

    private void updateGuidance(StrategyResult result, StrategyDataBundle data)
    {
        if (!OverlayDisplayState.from(config).showsMethodGuidance(
                    recommendationDetailsOverlay.hasRecommendation())
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

    private void refreshMethodGuidanceOverlay()
    {
        if (!OverlayDisplayState.from(config).showsMethodGuidance(
                    recommendationDetailsOverlay.hasRecommendation())
                || latestRecommendations.isEmpty() || latestData == null)
        {
            methodGuidanceOverlay.clear();
            return;
        }
        methodGuidanceOverlay.update(methodGuidanceService.build(
                latestRecommendations.get(0), latestData));
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

    private void syncProgressHistory(AccountSnapshot account)
    {
        String activeKey = accountProgressHistoryStore.getActiveProfileKey();
        if (Objects.equals(loadedProgressProfileKey, activeKey)
                && activeKey != null) return;
        progressHistory = activeKey == null
                ? new ProgressHistory() : accountProgressHistoryStore.load();
        progressAnalyticsService.beginSession(account);
        loadedProgressProfileKey = activeKey;
        progressCheckpointPending = false;
        lastProgressCheckpointAtMillis = System.currentTimeMillis();
    }

    private void archiveProgressSession()
    {
        if (loadedProgressProfileKey == null
                || !Objects.equals(loadedProgressProfileKey,
                        accountProgressHistoryStore.getActiveProfileKey()))
            return;
        progressHistory.archive(progressAnalyticsService.snapshot());
        savingProfileConfiguration = true;
        try
        {
            accountProgressHistoryStore.save(progressHistory);
        }
        finally
        {
            savingProfileConfiguration = false;
        }
        progressAnalyticsService.reset();
        progressCheckpointPending = false;
    }

    /**
     * Persists a replaceable preview of the active session at most once per
     * minute. A crash or late profile-change signal can therefore lose only
     * the newest interval without writing configuration for every XP drop.
     */
    private void checkpointProgressSession()
    {
        if (!progressCheckpointPending || loadedProgressProfileKey == null
                || !Objects.equals(loadedProgressProfileKey,
                        accountProgressHistoryStore.getActiveProfileKey()))
            return;
        long now = System.currentTimeMillis();
        if (now - lastProgressCheckpointAtMillis
                < PROGRESS_CHECKPOINT_INTERVAL_MILLIS) return;
        savingProfileConfiguration = true;
        try
        {
            accountProgressHistoryStore.save(progressHistory.checkpoint(
                    progressAnalyticsService.snapshot(now)));
            progressCheckpointPending = false;
            lastProgressCheckpointAtMillis = now;
        }
        finally
        {
            savingProfileConfiguration = false;
        }
    }

    private PlayerStrategyProfile effectiveStrategyProfile()
    {
        if (strategyProfile == null) strategyProfile = PlayerStrategyProfile.fromConfig(config);
        return strategyProfile;
    }

    private void updateAccountPanel()
    {
        if (panel == null) return;
        final long generation = uiGeneration.next();
        StrategyDataBundle data = strategyDataAssembler.read();
        if (data == null || data.getAccount() == null)
        {
            latestData = null;
            latestRecommendations = Collections.emptyList();
            methodGuidanceOverlay.clear();
            recommendationDetailsOverlay.clear();
            PlayerStrategyProfile profile = effectiveStrategyProfile();
            SwingUtilities.invokeLater(() ->
            {
                if (!uiGeneration.isCurrent(generation) || panel == null) return;
                panel.updateAccount("Waiting for login...", "Unknown", 0);
                panel.updateGoal(profile.getActiveGoal());
                panel.updateStrategy(profile.getStrategyMode(),
                        profile.getSessionIntent(), profile.getQuestTolerance());
            panel.updateRecommendations(Collections.emptyList());
            panel.updateOpportunities(Collections.emptyList());
            panel.updateProgress(null, null, null);
            });
            return;
        }

        latestData = data;
        syncPreferenceProfile();
        syncStrategyProfile();
        syncMilestoneProfile();
        syncRecommendationHistory();
        syncProgressHistory(data.getAccount());
        for (ProgressMilestone milestone : progressMilestoneDetector.observe(
                data, effectiveStrategyProfile().getActiveGoal(),
                System.currentTimeMillis()))
            progressCheckpointPending |= progressAnalyticsService
                    .recordMilestone(milestone);

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
                    RecommendationHistoryAction.COMPLETED);
            saveRecommendationHistory();

            trackedMilestone = null;
            saveTrackedMilestone();
            milestoneRewardOverlay.show(completion);
            progressCheckpointPending |= progressAnalyticsService
                    .recordMilestone(new ProgressMilestone(
                            "skill-checkpoint:" + completion.getActivityId()
                                    + ":" + completion.getSkill().name()
                                    .toLowerCase(java.util.Locale.ROOT) + ":"
                                    + completion.getTargetLevel(),
                            ProgressMilestoneType.SKILL_LEVEL,
                            completion.getTitle(),
                            "Completed the active Compass checkpoint.",
                            effectiveStrategyProfile().getActiveGoal().name(),
                            System.currentTimeMillis()));
        }

        PlayerStrategyProfile profile = effectiveStrategyProfile();
        StrategyResult result = evaluateAndStabilize(data, profile);
        latestRecommendations = new java.util.ArrayList<>(
                result.getRecommendations());
        updateTrackedMilestone(
                result.getRecommendations(),
                data.getCollectionLog());
        updateGuidance(result, data);
        updateProgressTarget(result);
        AccountSnapshot account = data.getAccount();

        SwingUtilities.invokeLater(() ->
        {
            if (!uiGeneration.isCurrent(generation) || panel == null) return;
            panel.updateAccount(
                    account.getPlayerName(),
                    account.getAccountTypeName(),
                    account.getMembershipStatus(),
                    account.getTotalLevel());
            panel.updateGoal(profile.getActiveGoal());
            panel.updateStrategy(
                    profile.getStrategyMode(),
                    profile.getSessionIntent(),
                    profile.getQuestTolerance());
            panel.updateRecommendations(result.getRecommendations());
            panel.updateOpportunities(result.getOpportunities());
            panel.updateProgress(progressAnalyticsService.snapshot(),
                    result.getPlan(), progressHistory);
        });
    }

    private void updateTrackedMilestone(
            List<Recommendation> recommendations,
            CollectionLogSnapshot collectionLog)
    {
        TrackedMilestone candidate = milestoneTracker.fromRecommendations(
                recommendations,
                collectionLog);
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

    private void acknowledgeFirstUse()
    {
        if (config.firstUseComplete()) return;
        configManager.setConfiguration(OsrsStrategistConfig.GROUP,
                CompassConfigKeys.FIRST_USE_COMPLETE, true);
        if (panel != null) panel.setFirstUseHintVisible(false);
    }

    private void saveStrategyProfile()
    {
        if (accountStrategyProfileStore.getActiveProfileKey() == null) return;
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

    private void updateOverlaySettings()
    {
        OverlayDisplayState state = OverlayDisplayState.from(config);
        if (panel != null)
            panel.setDetailsOverlayEnabled(state.showsDetails());
        if (!state.showsDetails()) recommendationDetailsOverlay.clear();
        if (!state.showsMethodGuidance(
                recommendationDetailsOverlay.hasRecommendation()))
        {
            methodGuidanceOverlay.clear();
            return;
        }
        if (latestData != null && !latestRecommendations.isEmpty())
            methodGuidanceOverlay.update(methodGuidanceService.build(
                    latestRecommendations.get(0), latestData));
    }

    private void registerOverlays()
    {
        if (!overlayLifecycle.beginRegistration()) return;
        overlayManager.add(milestoneRewardOverlay);
        overlayManager.add(methodGuidanceOverlay);
        overlayManager.add(recommendationDetailsOverlay);
    }

    private void unregisterOverlays()
    {
        if (!overlayLifecycle.beginRemoval()) return;
        overlayManager.remove(milestoneRewardOverlay);
        overlayManager.remove(methodGuidanceOverlay);
        overlayManager.remove(recommendationDetailsOverlay);
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
            default:
                return null;
        }
    }

    private BufferedImage loadPluginIcon()
    {
        return ImageUtil.loadImageResource(getClass(),
                "/gielinor-compass-icon.png");
    }
}
