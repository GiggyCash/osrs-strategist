package compass;
import static java.util.Collections.*;

import static compass.Text.get;

import com.google.inject.Binder;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.*;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.cluescrolls.ClueScrollPlugin;
import net.runelite.client.ui.*;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.*;

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
    /** Bursty varp/container events must not rebuild the full strategy each tick. */
    static final long STRATEGY_REFRESH_INTERVAL_MILLIS = 2_000L;

    @Inject private OsrsStrategistConfig config;
    @Inject private ConfigManager configManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private OverlayManager overlayManager;
    @Inject private StrategyDataAssembler strategyDataAssembler;
    @Inject private LiveItemStateReader liveItemStateReader;
    @Inject private StrategyEngine strategyEngine;
    @Inject private MethodGuidanceService methodGuidanceService;
    @Inject private AccountProfileStore accountProfileStore;
    @Inject private MilestoneTracker milestoneTracker;
    @Inject private SkillIconLoader skillIconLoader;
    @Inject private TrainingFatigueTracker trainingFatigueTracker;
    @Inject private MilestoneRewardOverlay milestoneRewardOverlay;
    @Inject private MethodGuidanceOverlay methodGuidanceOverlay;
    @Inject private RecommendationDetailsOverlay recommendationDetailsOverlay;
    @Inject private ProgressAnalyticsService progressAnalyticsService;
    @Inject private PlanContinuityService planContinuityService;

    private final PreferenceProfile preferenceProfile = new PreferenceProfile();
    private final RecommendationHistory recommendationHistory = new RecommendationHistory();
    private String loadedProfileKey;
    private String loadedProgressProfileKey;
    private boolean savingProfileConfiguration;
    private PlayerStrategyProfile strategyProfile;
    private TrackedMilestone trackedMilestone;
    private GameData latestData;
    private List<Recommendation> latestRecommendations = emptyList();
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
    private boolean pohRefreshPending;
    private boolean diaryRefreshPending;
    private boolean progressCheckpointPending;
    private long lastProgressCheckpointAtMillis;
    private long lastStrategyRefreshAtMillis = Long.MIN_VALUE;
    private long lastPohObservationAtMillis = Long.MIN_VALUE;
    private long lastFarmingObservationAtMillis = Long.MIN_VALUE;
    private final OverlayLifecycleGuard overlayLifecycle =
            new OverlayLifecycleGuard();
    private final RecommendationStabilizer recommendationStabilizer =
            new RecommendationStabilizer();

    @Override
    public void configure(Binder binder)
    {
        binder.requestStaticInjection(BundledCatalogLoader.class);
    }

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
        pohRefreshPending = true;
        syncProfileState();
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
        loadedProfileKey = null;
        loadedProgressProfileKey = null;
        savingProfileConfiguration = false;
        strategyProfile = null;
        trackedMilestone = null;
        latestData = null;
        latestRecommendations = emptyList();
        latestPlan = null;
        varbitRefreshPending = false;
        accountRefreshPending = false;
        lastStrategyRefreshAtMillis = Long.MIN_VALUE;
        lastPohObservationAtMillis = Long.MIN_VALUE;
        lastFarmingObservationAtMillis = Long.MIN_VALUE;
        pohRefreshPending = false;
        diaryRefreshPending = false;
        panel = null;
        navButton = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event == null) return;
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            // Login emits profile, inventory, equipment, varbit, quest and
            // object bursts close together. Let the next game tick assemble
            // one complete snapshot instead of synchronously evaluating here
            // and then reranking the same state again two seconds later.
            accountRefreshPending = true;
            pohRefreshPending = true;
            return;
        }
        if (progressAnalyticsService != null)
            progressAnalyticsService.pause(System.currentTimeMillis());
        pohRefreshPending = true;
        // Publish the waiting state once when leaving an active character;
        // intermediate loading-state events should not repeat full reads.
        if (latestData != null) updateAccountPanel();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        var now = System.currentTimeMillis();
        var accessChanged = strategyDataAssembler.observeAccess();
        boolean farmChanged = consumeFarmingObservation(now)
                && strategyDataAssembler.observeFarmingPatches();
        boolean pohChanged = consumePohRefreshPending(now)
                && strategyDataAssembler.observePoh();
        boolean diaryChanged = consumeDiaryRefreshPending()
                && strategyDataAssembler.observeOpenDiary();
        checkpointProgressSession();
        if (accessChanged || farmChanged || pohChanged || diaryChanged)
            accountRefreshPending = true;
        if (consumeStrategyRefreshPending(now))
            updateAccountPanel();
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event != null && event.getGroupId() == InterfaceID.JOURNALSCROLL)
            diaryRefreshPending = true;
    }

    /**
     * Quest, diary, Slayer, and several unlock states are varbit-backed. Coalesce
     * their often-bursty changes into one read on the following game tick.
     */
    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        varbitRefreshPending = true;
        // Entering build mode and furniture state transitions are varbit/object
        // backed. Coalesce them into one ownership-gated scene scan.
        pohRefreshPending = true;
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        pohRefreshPending = true;
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        pohRefreshPending = true;
    }

    boolean consumePohRefreshPending(long nowMillis)
    {
        if (!pohRefreshPending) return false;
        if (!intervalElapsed(nowMillis, lastPohObservationAtMillis))
            return false;
        pohRefreshPending = false;
        lastPohObservationAtMillis = nowMillis;
        return true;
    }

    boolean consumeDiaryRefreshPending()
    {
        var pending = diaryRefreshPending;
        diaryRefreshPending = false;
        return pending;
    }

    /**
     * Coalesces unrelated varbit bursts and repeated inventory mutations while
     * retaining the pending refresh. Delaying a rerank by at most two seconds
     * avoids client-thread catalog scans on every 600 ms game tick.
     */
    boolean consumeStrategyRefreshPending(long nowMillis)
    {
        if (!varbitRefreshPending && !accountRefreshPending) return false;
        if (!intervalElapsed(nowMillis, lastStrategyRefreshAtMillis))
            return false;
        varbitRefreshPending = false;
        accountRefreshPending = false;
        lastStrategyRefreshAtMillis = nowMillis;
        return true;
    }

    boolean consumeFarmingObservation(long nowMillis)
    {
        if (!intervalElapsed(nowMillis, lastFarmingObservationAtMillis))
            return false;
        lastFarmingObservationAtMillis = nowMillis;
        return true;
    }

    private static boolean intervalElapsed(long nowMillis, long previousMillis)
    {
        return previousMillis == Long.MIN_VALUE
                || nowMillis < previousMillis
                || nowMillis - previousMillis >= STRATEGY_REFRESH_INTERVAL_MILLIS;
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        var progressChanged = progressAnalyticsService.record(event);
        if (progressChanged)
        {
            progressCheckpointPending = true;
            updateProgressPanel();
        }
        var profile = effectiveStrategyProfile();
        TrainingFatigueTracker.FatigueSignal fatigue = trainingFatigueTracker.record(
                event.getSkill(), event.getXp(), profile.mode());
        if (fatigue.isPresent())
        {
            preferenceProfile.addTemporaryScoreAdjustment(
                    fatigue.activityId,
                    fatigue.getScoreDelta(),
                    fatigue.getDurationMillis());
        }
        if (fatigue.isPresent() || latestData == null
                || latestData.account() == null
                || latestData.account().level(event.getSkill())
                        != event.getLevel())
        {
            accountRefreshPending = true;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event == null || !isStrategicContainer(event.getContainerId())) return;
        if (event.getContainerId()
                        == InventoryID.INV_GROUP_TEMP
                && liveItemStateReader != null)
        {
            liveItemStateReader.observeGroupStorage(event.getItemContainer());
        }
        accountRefreshPending = true;
    }

    static boolean isStrategicContainer(int containerId)
    {
        return containerId == InventoryID.INV
                || containerId == InventoryID.WORN
                || containerId == InventoryID.BANK
                || containerId
                        == InventoryID.INV_GROUP_TEMP;
    }

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    {
        if (savingProfileConfiguration) return;
        // Fail closed before reading the new profile. No queued Swing callback,
        // cached bundle, preference weight, or strategy choice from the prior
        // character may survive even for a single refresh generation.
        uiGeneration.invalidate();
        loadedProfileKey = null;
        if (Objects.equals(loadedProgressProfileKey,
                accountProfileStore.activeProfileKey()))
            archiveProgressSession();
        loadedProgressProfileKey = null;
        progressCheckpointPending = false;
        lastProgressCheckpointAtMillis = 0L;
        lastStrategyRefreshAtMillis = Long.MIN_VALUE;
        lastPohObservationAtMillis = Long.MIN_VALUE;
        lastFarmingObservationAtMillis = Long.MIN_VALUE;
        latestRecommendations = emptyList();
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
        varbitRefreshPending = false;
        accountRefreshPending = false;
        methodGuidanceOverlay.clear();
        recommendationDetailsOverlay.clear();
        syncProfileState();
        // RuneScapeProfileChanged and LOGGED_IN commonly arrive in the same
        // burst. State above is already cleared fail-closed; defer the one
        // expensive account assembly/rank to the coalesced game-tick path.
        accountRefreshPending = true;
        pohRefreshPending = true;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!OsrsStrategistConfig.GROUP.equals(event.getGroup())) return;
        var key = event.getKey();
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
        latestRecommendations = emptyList();
        latestPlan = null;
        if (panel != null) panel.closeDetails();
        recommendationDetailsOverlay.clear();
        updateAccountPanel();
    }

    /** Clear only recommendation learning for the active RuneScape profile. */
    boolean resetLearnedFeedbackForActiveCharacter()
    {
        syncProfileState();
        var activeKey = accountProfileStore.activeProfileKey();
        if (activeKey == null) return false;

        persist(accountProfileStore::clearPreferences);
        preferenceProfile.clear();
        loadedProfileKey = activeKey;

        // Bypass hysteresis for this deliberate recovery action. Completion
        // history, goal/settings, milestone state, and observed account data
        // are intentionally left untouched.
        latestRecommendations = emptyList();
        accountRefreshPending = true;
        pohRefreshPending = true;
        if (recommendationDetailsOverlay != null)
            recommendationDetailsOverlay.clear();
        refreshStrategyImmediately();
        return true;
    }

    void applyRecommendationFeedback(String activityId, FeedbackAction action)
    {
        if (activityId == null || action == null) return;
        syncProfileState();
        Recommendation current = null;
        for (Recommendation recommendation : latestRecommendations)
            if (activityId.equals(recommendation.id))
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

        var historyAction = historyAction(action);
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
                    ? null : latestData.account();
            recommendationDetailsOverlay.showRecommendation(recommendation,
                    GoalRecommendationContext.assess(
                            effectiveStrategyProfile().goal(),
                            recommendation,
                            account == null ? Membership.UNKNOWN
                                    : account.membership()));
            methodGuidanceOverlay.clear();
        }
    }

    private void refreshStrategyImmediately()
    {
        if (panel == null || latestData == null) return;
        lastStrategyRefreshAtMillis = System.currentTimeMillis();
        final long generation = uiGeneration.next();
        var profile = effectiveStrategyProfile();
        var result = evaluateAndStabilize(latestData, profile);
        latestRecommendations = new ArrayList<>(
                result.recommendations);
        updateTrackedMilestone(
                result.recommendations,
                latestData.collectionLog());
        updateGuidance(result, latestData);
        updateProgressTarget(result);

        Runnable update = () ->
        {
            if (!uiGeneration.isCurrent(generation) || panel == null) return;
            panel.updateRecommendations(result.recommendations);
            panel.updateOpportunities(result.getOpportunities());
            panel.updateProgress(progressAnalyticsService.snapshot(),
                    result.getPlan(), progressHistory);
            panel.revalidate();
            panel.repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) update.run();
        else SwingUtilities.invokeLater(update);
    }

    private StrategyResult evaluate(GameData data, PlayerStrategyProfile profile)
    {
        return strategyEngine.evaluate(
                data,
                profile.mode(),
                profile.intent(),
                profile.questTolerance,
                profile.goal(),
                profile.usesGroupStorage(),
                profile.collectionist(),
                profile.allowsWilderness(),
                preferenceProfile);
    }

    private StrategyResult evaluateAndStabilize(
            GameData data, PlayerStrategyProfile profile)
    {
        StrategyResult fresh = recommendationStabilizer.stabilize(
                latestRecommendations, evaluate(data, profile));
        StrategyContext context = new StrategyContext(data,
                profile.mode(), profile.intent(),
                profile.questTolerance, profile.goal(),
                profile.usesGroupStorage(), profile.collectionist(),
                profile.allowsWilderness(), preferenceProfile);
        var previousPlan = latestPlan;
        latestPlan = planContinuityService.reconcile(previousPlan,
                fresh.getPlan(), context, fresh.recommendations);
        if (previousPlan != null && previousPlan.matchesContext(context)
                && previousPlan.getCurrentStep().isComplete(data)
                && latestPlan != null
                && !previousPlan.getCurrentStep().id.equals(
                        latestPlan.getCurrentStep().id))
            progressCheckpointPending |= progressAnalyticsService
                    .recordMilestone(new ProgressMilestone(
                            "plan-step:"
                                    + previousPlan.getCurrentStep().id,
                            ProgressMilestoneType.PLAN_STEP,
                            previousPlan.getCurrentStep().getObjective()
                                    + " complete",
                            get(410),
                            profile.goal().name(),
                            System.currentTimeMillis()));
        return fresh.withPlan(latestPlan);
    }

    private void updateProgressTarget(StrategyResult result)
    {
        if (result == null || result.recommendations.isEmpty())
        {
            progressAnalyticsService.clearTarget();
            return;
        }
        var top = result.recommendations.get(0);
        var plan = top.plan();
        if (plan == null || plan.method() == null
                || plan.method().getSkill() == null
                || top.getCurrentExecutionTargetLevel() < 2)
        {
            progressAnalyticsService.clearTarget();
            return;
        }
        progressAnalyticsService.setTarget(new ProgressTarget(top.id,
                plan.method().id, plan.method().getSkill(),
                top.getCurrentExecutionTargetLevel()));
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

    private void updateGuidance(StrategyResult result, GameData data)
    {
        if (!OverlayDisplayState.from(config).showsMethodGuidance(
                    recommendationDetailsOverlay.hasRecommendation())
                || result == null
                || result.recommendations.isEmpty())
        {
            methodGuidanceOverlay.clear();
            return;
        }
        GuidanceChecklist checklist = methodGuidanceService.build(
                result.recommendations.get(0), data);
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

    private void syncProfileState()
    {
        var activeKey = accountProfileStore.activeProfileKey();
        if (Objects.equals(loadedProfileKey, activeKey)) return;
        preferenceProfile.clear();
        recommendationHistory.clear();
        if (activeKey == null)
        {
            loadedProfileKey = null;
            strategyProfile = PlayerStrategyProfile.fromConfig(config);
            trackedMilestone = null;
            return;
        }
        accountProfileStore.loadPreferences(preferenceProfile);
        strategyProfile = accountProfileStore.loadStrategy(
                PlayerStrategyProfile.fromConfig(config));
        trackedMilestone = accountProfileStore.loadMilestone();
        accountProfileStore.loadRecommendations(recommendationHistory);
        loadedProfileKey = activeKey;
    }

    private void syncProgressHistory(AccountSnapshot account)
    {
        var activeKey = accountProfileStore.activeProfileKey();
        if (Objects.equals(loadedProgressProfileKey, activeKey)
                && activeKey != null) return;
        progressHistory = activeKey == null
                ? new ProgressHistory() : accountProfileStore.loadProgress();
        progressAnalyticsService.beginSession(account);
        loadedProgressProfileKey = activeKey;
        progressCheckpointPending = false;
        lastProgressCheckpointAtMillis = System.currentTimeMillis();
    }

    private void archiveProgressSession()
    {
        if (loadedProgressProfileKey == null
                || !Objects.equals(loadedProgressProfileKey,
                        accountProfileStore.activeProfileKey()))
            return;
        progressHistory.archive(progressAnalyticsService.snapshot());
        persist(() -> accountProfileStore.saveProgress(progressHistory));
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
                        accountProfileStore.activeProfileKey()))
            return;
        var now = System.currentTimeMillis();
        if (now - lastProgressCheckpointAtMillis
                < PROGRESS_CHECKPOINT_INTERVAL_MILLIS) return;
        persist(() -> accountProfileStore.saveProgress(
                progressHistory.checkpoint(progressAnalyticsService.snapshot(now))));
        progressCheckpointPending = false;
        lastProgressCheckpointAtMillis = now;
    }

    private PlayerStrategyProfile effectiveStrategyProfile()
    {
        if (strategyProfile == null) strategyProfile = PlayerStrategyProfile.fromConfig(config);
        return strategyProfile;
    }

    private void updateAccountPanel()
    {
        if (panel == null) return;
        lastStrategyRefreshAtMillis = System.currentTimeMillis();
        final long generation = uiGeneration.next();
        var data = strategyDataAssembler.read();
        if (data == null || data.account() == null)
        {
            latestData = null;
            latestRecommendations = emptyList();
            methodGuidanceOverlay.clear();
            recommendationDetailsOverlay.clear();
            var profile = effectiveStrategyProfile();
            SwingUtilities.invokeLater(() ->
            {
                if (!uiGeneration.isCurrent(generation) || panel == null) return;
                panel.updateAccount("Waiting for login...", "Unknown", 0);
                panel.updateGoal(profile.goal());
                panel.updateStrategy(profile.mode(),
                        profile.intent(), profile.questTolerance);
            panel.updateRecommendations(emptyList());
            panel.updateOpportunities(emptyList());
            panel.updateProgress(null, null, null);
            });
            return;
        }

        latestData = data;
        syncProfileState();
        syncProgressHistory(data.account());
        for (ProgressMilestone milestone : progressMilestoneDetector.observe(
                data, effectiveStrategyProfile().goal(),
                System.currentTimeMillis()))
            progressCheckpointPending |= progressAnalyticsService
                    .recordMilestone(milestone);

        var completedCheckpoint = trackedMilestone;
        MilestoneCompletion completion = milestoneTracker.detectCompletion(
                completedCheckpoint, data.account());
        if (completion != null)
        {
            if (completedCheckpoint == null
                    || !completedCheckpoint.progressionProtected)
            {
                preferenceProfile.addTemporaryScoreAdjustment(
                        completion.activityId, COMPLETION_VARIETY_PENALTY,
                        COMPLETION_VARIETY_DURATION_MILLIS);
                savePreferenceProfile();
            }

            recommendationHistory.add(
                    completion.activityId,
                    completion.title,
                    RecommendationHistoryAction.COMPLETED);
            saveRecommendationHistory();

            trackedMilestone = null;
            saveTrackedMilestone();
            milestoneRewardOverlay.show(completion);
            progressCheckpointPending |= progressAnalyticsService
                    .recordMilestone(new ProgressMilestone(
                            "skill-checkpoint:" + completion.activityId
                                    + ":" + completion.getSkill().name()
                                    .toLowerCase(Locale.ROOT) + ":"
                                    + completion.targetLevel,
                            ProgressMilestoneType.SKILL_LEVEL,
                            completion.title,
                            get(411),
                            effectiveStrategyProfile().goal().name(),
                            System.currentTimeMillis()));
        }

        var profile = effectiveStrategyProfile();
        var result = evaluateAndStabilize(data, profile);
        latestRecommendations = new ArrayList<>(
                result.recommendations);
        updateTrackedMilestone(
                result.recommendations,
                data.collectionLog());
        updateGuidance(result, data);
        updateProgressTarget(result);
        var account = data.account();

        SwingUtilities.invokeLater(() ->
        {
            if (!uiGeneration.isCurrent(generation) || panel == null) return;
            panel.updateAccount(
                    account.playerName,
                    account.getAccountTypeName(),
                    account.membership(),
                    account.getTotalLevel());
            panel.updateGoal(profile.goal());
            panel.updateStrategy(
                    profile.mode(),
                    profile.intent(),
                    profile.questTolerance);
            panel.updateRecommendations(result.recommendations);
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
        persist(() -> accountProfileStore.savePreferences(preferenceProfile));
        loadedProfileKey = accountProfileStore.activeProfileKey();
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
        if (accountProfileStore.activeProfileKey() == null) return;
        persist(() -> accountProfileStore.saveStrategy(strategyProfile));
        loadedProfileKey = accountProfileStore.activeProfileKey();
    }

    private void updateOverlaySettings()
    {
        var state = OverlayDisplayState.from(config);
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
        persist(() -> accountProfileStore.saveRecommendations(
                recommendationHistory));
        loadedProfileKey = accountProfileStore.activeProfileKey();
    }

    private void saveTrackedMilestone()
    {
        persist(() -> accountProfileStore.saveMilestone(trackedMilestone));
        loadedProfileKey = accountProfileStore.activeProfileKey();
    }

    private void persist(Runnable write)
    {
        savingProfileConfiguration = true;
        try { write.run(); }
        finally { savingProfileConfiguration = false; }
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
        return loadPluginIcon(getClass(), "/gielinor-compass-icon.png");
    }

    static BufferedImage loadPluginIcon(Class<?> owner, String resource)
    {
        try
        {
            var icon = ImageUtil.loadImageResource(owner, resource);
            if (icon != null) return icon;
        }
        catch (RuntimeException ignored)
        {
            // A packaging mistake must not prevent RuneLite from starting.
        }
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    }
}
