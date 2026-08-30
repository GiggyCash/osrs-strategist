package com.udderlywet.osrsstrategist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import net.runelite.api.Skill;
import net.runelite.client.ui.PluginPanel;

/**
 * RuneLite sidebar for Compass.
 *
 * <p>All player-facing variable-length copy uses real wrapping text components.
 * Swing's HTML JLabel renderer is deliberately avoided for recommendation copy
 * because it can paint beyond the declared CSS width and clip glyphs at the
 * RuneLite sidebar boundary.</p>
 */
public class OsrsStrategistPanel extends PluginPanel
{
    private static final int CONTENT_PADDING = 8;
    private static final int CARD_HORIZONTAL_INSET = 20;
    private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH
            - (PluginPanel.BORDER_OFFSET * 2)
            - (CONTENT_PADDING * 2);
    private static final int INNER_WIDTH = CONTENT_WIDTH - CARD_HORIZONTAL_INSET;
    private static final int TEXT_WIDTH = INNER_WIDTH - 2;

    private static final float BODY_FONT_SIZE = 14f;
    private static final float MUTED_FONT_SIZE = 13f;
    private static final float BUTTON_FONT_SIZE = 13f;
    private static final float EYEBROW_FONT_SIZE = 12f;
    private static final float EMPHASIS_FONT_SIZE = 15f;

    private final SkillIconLoader skillIconLoader;
    private final BiConsumer<String, FeedbackAction> feedbackHandler;
    private final Consumer<Recommendation> detailsHandler;
    private final Runnable refreshHandler;
    private final Runnable firstUseHandler;
    private final String supportUrl;
    private final Consumer<String> supportBrowser;

    private final JLabel accountName = label("Waiting for login...");
    private final JLabel accountMeta = mutedLabel("Unknown • -- / 2376");
    private final JLabel activeGoal = label("Goal: Automatic");
    private final JLabel strategySummary = mutedLabel(
            "Mode: Balanced<br>Session: Pick for me<br>Optional quests: Normal"
    );
    private final JTextArea firstUseHint = wrappingArea(
            "Compass starts with sensible defaults. Change Goal or Session only when you want to steer the plan.",
            MUTED_FONT_SIZE, StrategistTheme.MUTED_TEXT, false);

    private final JPanel milestoneBanner = cardPanel(true);
    private final JLabel milestoneTitle = label("Milestone complete!");
    private final JTextArea milestoneBody = wrappingArea(
            "", MUTED_FONT_SIZE, StrategistTheme.MUTED_TEXT, false);
    private Timer milestoneHideTimer;

    private final JPanel recommendationCard = cardPanel(true);
    private final JLabel recommendationIcon = new JLabel();
    private final JLabel recommendationEyebrow = inlineMutedLabel("NEXT MOVE");
    private final JTextArea recommendationTitle = wrappingArea(
            "Finding your next move...", EMPHASIS_FONT_SIZE, StrategistTheme.TEXT, true);
    private final JLabel progressText = mutedLabel("");
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JTextArea recommendationBody = wrappingArea(
            "", BODY_FONT_SIZE, StrategistTheme.TEXT, false);
    private final JTextArea feedbackStatus = wrappingArea(
            "", MUTED_FONT_SIZE, StrategistTheme.MUTED_TEXT, false);

    private final JTextArea alternativeOne = wrappingArea(
            "", BODY_FONT_SIZE, StrategistTheme.TEXT, false);
    private final JTextArea alternativeTwo = wrappingArea(
            "", BODY_FONT_SIZE, StrategistTheme.TEXT, false);
    private final JTextArea opportunityOne = wrappingArea(
            "", BODY_FONT_SIZE, StrategistTheme.TEXT, false);
    private final JTextArea opportunityTwo = wrappingArea(
            "", BODY_FONT_SIZE, StrategistTheme.TEXT, false);
    private final JPanel alternativesCard = cardPanel(false);
    private final JPanel opportunitiesCard = cardPanel(false);

    private final JButton detailsButton = actionButton("Details");
    private final JButton laterButton = actionButton("Later");
    private final JButton notTodayButton = actionButton("Not Today");
    private final JButton dislikeButton = actionButton("Dislike");
    private final JButton refreshButton = actionButton("Refresh Plan");
    private final JButton supportButton = actionButton("Support Compass");
    private final JButton doNextViewButton = actionButton("Do Next");
    private final JButton progressViewButton = actionButton("Progress");
    private final JPanel recommendationControls = new JPanel();
    private final JPanel feedbackPanel = new JPanel();
    private final CardLayout viewLayout = new CardLayout();
    private final JPanel viewCards = new JPanel(viewLayout);
    private final ProgressViewPanel progressView = new ProgressViewPanel();

    private Recommendation currentRecommendation;
    private boolean detailsVisible;
    private String riskAcknowledgedRecommendationId;
    private boolean detailsOverlayEnabled = true;
    private GoalType selectedGoal = GoalType.AUTOMATIC;
    private MembershipStatus membership = MembershipStatus.UNKNOWN;

    public OsrsStrategistPanel(
            BiConsumer<String, FeedbackAction> feedbackHandler,
            SkillIconLoader skillIconLoader)
    {
        this(feedbackHandler, skillIconLoader, recommendation -> { },
                () -> { }, () -> { }, SupportLinks.SUPPORT_URL,
                value -> { });
    }

    public OsrsStrategistPanel(
            BiConsumer<String, FeedbackAction> feedbackHandler,
            SkillIconLoader skillIconLoader,
            Consumer<Recommendation> detailsHandler)
    {
        this(feedbackHandler, skillIconLoader, detailsHandler,
                () -> { }, () -> { }, SupportLinks.SUPPORT_URL,
                value -> { });
    }

    OsrsStrategistPanel(
            BiConsumer<String, FeedbackAction> feedbackHandler,
            SkillIconLoader skillIconLoader,
            Consumer<Recommendation> detailsHandler,
            Runnable refreshHandler,
            Runnable firstUseHandler,
            String supportUrl,
            Consumer<String> supportBrowser)
    {
        this.feedbackHandler = feedbackHandler;
        this.skillIconLoader = skillIconLoader;
        this.detailsHandler = detailsHandler == null
                ? recommendation -> { }
                : detailsHandler;
        this.refreshHandler = refreshHandler == null ? () -> { } : refreshHandler;
        this.firstUseHandler = firstUseHandler == null
                ? () -> { } : firstUseHandler;
        this.supportUrl = supportUrl;
        this.supportBrowser = supportBrowser == null ? value -> { } : supportBrowser;

        setLayout(new BorderLayout());
        setBackground(StrategistTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(StrategistTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(
                8, CONTENT_PADDING, 12, CONTENT_PADDING));
        buildHeader(content);
        buildStrategyCard(content);
        buildMilestoneBanner(content);
        buildRecommendationCard(content);
        buildSecondaryCards(content);
        buildFooter(content);
        JPanel primary = new JPanel(new BorderLayout());
        primary.setBackground(StrategistTheme.BACKGROUND);
        primary.add(content, BorderLayout.NORTH);

        JPanel navigation = new JPanel(new GridLayout(1, 2, 6, 0));
        navigation.setBackground(StrategistTheme.BACKGROUND);
        navigation.setBorder(BorderFactory.createEmptyBorder(
                8, CONTENT_PADDING, 0, CONTENT_PADDING));
        navigation.add(doNextViewButton);
        navigation.add(progressViewButton);
        doNextViewButton.addActionListener(event ->
                viewLayout.show(viewCards, "do-next"));
        progressViewButton.addActionListener(event ->
                viewLayout.show(viewCards, "progress"));

        viewCards.setBackground(StrategistTheme.BACKGROUND);
        viewCards.add(primary, "do-next");
        viewCards.add(progressView, "progress");
        add(navigation, BorderLayout.NORTH);
        add(viewCards, BorderLayout.CENTER);
    }

    private void buildHeader(JPanel content)
    {
        JLabel title = label("GIELINOR COMPASS");
        title.setForeground(StrategistTheme.GOLD);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        content.add(title);
        content.add(Box.createVerticalStrut(4));

        JLabel subtitle = mutedLabel("Your account. Your next move.");
        subtitle.setFont(subtitle.getFont().deriveFont(MUTED_FONT_SIZE));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(11));

        JPanel accountCard = cardPanel(false);
        accountName.setFont(accountName.getFont().deriveFont(Font.BOLD, EMPHASIS_FONT_SIZE));
        accountCard.add(accountName);
        accountCard.add(Box.createVerticalStrut(5));
        accountCard.add(accountMeta);
        content.add(accountCard);
        content.add(Box.createVerticalStrut(9));
    }

    private void buildStrategyCard(JPanel content)
    {
        JPanel card = cardPanel(false);
        activeGoal.setForeground(StrategistTheme.GOLD);
        activeGoal.setFont(activeGoal.getFont().deriveFont(Font.BOLD, EMPHASIS_FONT_SIZE));
        card.add(eyebrow("STRATEGY"));
        card.add(Box.createVerticalStrut(7));
        card.add(activeGoal);
        card.add(Box.createVerticalStrut(7));
        card.add(strategySummary);
        content.add(card);
        content.add(Box.createVerticalStrut(9));

        setWrappedText(firstUseHint, firstUseHint.getText(), TEXT_WIDTH);
        content.add(firstUseHint);
        content.add(Box.createVerticalStrut(9));
    }

    private void buildMilestoneBanner(JPanel content)
    {
        milestoneTitle.setForeground(StrategistTheme.SUCCESS);
        milestoneTitle.setFont(milestoneTitle.getFont().deriveFont(Font.BOLD, EMPHASIS_FONT_SIZE));
        milestoneBanner.add(milestoneTitle);
        milestoneBanner.add(Box.createVerticalStrut(5));
        setWrappedText(milestoneBody, "", TEXT_WIDTH);
        milestoneBanner.add(milestoneBody);
        milestoneBanner.setVisible(false);
        content.add(milestoneBanner);
        content.add(Box.createVerticalStrut(9));
    }

    private void buildRecommendationCard(JPanel content)
    {
        content.add(eyebrow("DO NEXT"));
        content.add(Box.createVerticalStrut(6));

        JPanel identityRow = new JPanel(new BorderLayout(7, 0));
        identityRow.setOpaque(false);
        identityRow.setAlignmentX(LEFT_ALIGNMENT);
        identityRow.setMaximumSize(new Dimension(INNER_WIDTH, 34));

        recommendationIcon.setHorizontalAlignment(SwingConstants.CENTER);
        recommendationIcon.setVerticalAlignment(SwingConstants.CENTER);
        recommendationIcon.setPreferredSize(new Dimension(26, 26));
        recommendationEyebrow.setFont(
                recommendationEyebrow.getFont().deriveFont(Font.BOLD, EYEBROW_FONT_SIZE));
        identityRow.add(recommendationIcon, BorderLayout.WEST);
        identityRow.add(recommendationEyebrow, BorderLayout.CENTER);

        recommendationCard.add(identityRow);
        recommendationCard.add(Box.createVerticalStrut(6));
        setWrappedText(recommendationTitle, "Analyzing account...", TEXT_WIDTH);
        recommendationCard.add(recommendationTitle);
        recommendationCard.add(Box.createVerticalStrut(9));

        progressBar.setAlignmentX(LEFT_ALIGNMENT);
        progressBar.setPreferredSize(new Dimension(INNER_WIDTH, 8));
        progressBar.setMaximumSize(new Dimension(INNER_WIDTH, 8));
        progressBar.setMinimumSize(new Dimension(INNER_WIDTH, 8));
        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(false);
        progressBar.setBackground(StrategistTheme.DIVIDER);
        progressBar.setForeground(StrategistTheme.GOLD);
        recommendationCard.add(progressText);
        recommendationCard.add(Box.createVerticalStrut(4));
        recommendationCard.add(progressBar);
        recommendationCard.add(Box.createVerticalStrut(11));

        setWrappedText(recommendationBody, "", TEXT_WIDTH);
        recommendationCard.add(recommendationBody);
        recommendationCard.add(Box.createVerticalStrut(12));

        makeFullWidth(detailsButton, 34);

        recommendationControls.setLayout(new BoxLayout(
                recommendationControls, BoxLayout.Y_AXIS));
        recommendationControls.setOpaque(false);
        recommendationControls.setAlignmentX(LEFT_ALIGNMENT);
        recommendationControls.add(detailsButton);
        recommendationControls.add(Box.createVerticalStrut(9));
        feedbackPanel.setLayout(new BoxLayout(feedbackPanel, BoxLayout.Y_AXIS));
        feedbackPanel.setOpaque(false);
        feedbackPanel.setAlignmentX(LEFT_ALIGNMENT);
        feedbackPanel.add(eyebrow("FEEDBACK"));
        feedbackPanel.add(Box.createVerticalStrut(6));
        JPanel feedbackGrid = new JPanel(new GridLayout(2, 2, 6, 6));
        feedbackGrid.setOpaque(false);
        feedbackGrid.setAlignmentX(LEFT_ALIGNMENT);
        feedbackGrid.setMaximumSize(new Dimension(INNER_WIDTH, 74));
        feedbackGrid.add(laterButton);
        feedbackGrid.add(notTodayButton);
        feedbackGrid.add(dislikeButton);
        feedbackPanel.add(feedbackGrid);
        feedbackPanel.add(Box.createVerticalStrut(8));
        setWrappedText(feedbackStatus, "", TEXT_WIDTH);
        feedbackPanel.add(feedbackStatus);
        recommendationControls.add(feedbackPanel);
        recommendationCard.add(recommendationControls);

        detailsButton.addActionListener(event -> toggleDetails());
        laterButton.addActionListener(event -> submitFeedback(FeedbackAction.LATER));
        notTodayButton.addActionListener(event -> submitFeedback(FeedbackAction.NOT_TODAY));
        dislikeButton.addActionListener(event -> submitFeedback(FeedbackAction.DISLIKE));
        setRecommendationButtonsEnabled(false);

        content.add(recommendationCard);
        content.add(Box.createVerticalStrut(11));
    }

    private void buildSecondaryCards(JPanel content)
    {
        alternativesCard.add(eyebrow("OTHER GOOD OPTIONS"));
        alternativesCard.add(Box.createVerticalStrut(8));
        setWrappedText(alternativeOne, "", TEXT_WIDTH);
        alternativesCard.add(alternativeOne);
        alternativesCard.add(Box.createVerticalStrut(9));
        setWrappedText(alternativeTwo, "", TEXT_WIDTH);
        alternativesCard.add(alternativeTwo);
        alternativesCard.setVisible(false);
        content.add(alternativesCard);
        content.add(Box.createVerticalStrut(9));

        opportunitiesCard.add(eyebrow("OPPORTUNITIES"));
        opportunitiesCard.add(Box.createVerticalStrut(8));
        setWrappedText(opportunityOne, "", TEXT_WIDTH);
        opportunitiesCard.add(opportunityOne);
        opportunitiesCard.add(Box.createVerticalStrut(9));
        setWrappedText(opportunityTwo, "", TEXT_WIDTH);
        opportunitiesCard.add(opportunityTwo);
        opportunitiesCard.setVisible(false);
        content.add(opportunitiesCard);
    }

    private void buildFooter(JPanel content)
    {
        content.add(Box.createVerticalStrut(9));
        makeFullWidth(refreshButton, 32);
        refreshButton.addActionListener(event ->
        {
            acknowledgeFirstUse();
            refreshHandler.run();
        });
        content.add(refreshButton);

        content.add(Box.createVerticalStrut(12));
        Dimension supportSize = new Dimension(CONTENT_WIDTH, 34);
        supportButton.setPreferredSize(supportSize);
        supportButton.setMinimumSize(supportSize);
        supportButton.setMaximumSize(supportSize);
        supportButton.setForeground(StrategistTheme.TEXT);
        supportButton.setBorderPainted(true);
        supportButton.setContentAreaFilled(true);
        supportButton.setVisible(SupportLinks.isConfigured(supportUrl));
        supportButton.setAlignmentX(LEFT_ALIGNMENT);
        supportButton.addActionListener(event ->
                SupportLinks.openIfConfigured(supportUrl, supportBrowser));
        content.add(supportButton);
    }

    public void updateAccount(String name, String type, int total)
    {
        updateAccount(name, type, "Unknown access", total);
    }

    public void updateAccount(
            String name,
            String type,
            String membership,
            int total)
    {
        this.membership = membershipFromDisplay(membership);
        accountName.setText(html(escape(name)));
        accountMeta.setText(html(
                escape(type)
                        + "<br>"
                        + escape(membership)
                        + "<br>"
                        + (total > 0 ? total : "--")
                        + " / 2376 total"));
    }

    public void updateAccount(String name, String type,
            MembershipStatus membership, int total)
    {
        this.membership = membership == null
                ? MembershipStatus.UNKNOWN : membership;
        updateAccount(name, type, this.membership.getDisplayName(), total);
    }

    public void updateGoal(GoalType goal)
    {
        GoalType safeGoal = goal == null ? GoalType.AUTOMATIC : goal;
        selectedGoal = safeGoal;
        activeGoal.setText(html("Goal: "
                + GoalRecommendationContext.displayName(safeGoal)));
        renderRecommendationBody();
    }

    public void updateProgress(
            ProgressSessionSnapshot snapshot, StrategicPlan plan)
    {
        updateProgress(snapshot, plan, null);
    }

    public void updateProgress(ProgressSessionSnapshot snapshot,
            StrategicPlan plan, ProgressHistory history)
    {
        progressView.setSnapshot(snapshot);
        progressView.setPlan(plan);
        progressView.setHistory(history);
    }

    ProgressViewPanel getProgressView()
    {
        return progressView;
    }

    public void updateStrategy(StrategyMode mode, QuestTolerance tolerance)
    {
        updateStrategy(mode, SessionIntent.PICK_FOR_ME, tolerance);
    }

    public void updateStrategy(
            StrategyMode mode,
            SessionIntent intent,
            QuestTolerance tolerance)
    {
        strategySummary.setText(html(
                "Mode: " + prettyName(mode.name())
                        + "<br>Session: " + intent
                        + "<br>Optional quests: " + prettyName(tolerance.name())));
    }

    public void updateRecommendations(List<Recommendation> recommendations)
    {
        if (recommendations == null || recommendations.isEmpty())
        {
            recommendations = java.util.Collections.singletonList(
                    FallbackRecommendationFactory.forState(null));
        }

        Recommendation best = recommendations.get(0);
        String previousId = currentRecommendation == null
                ? null
                : currentRecommendation.getId();

        boolean recommendationChanged = previousId == null
                || !previousId.equals(best.getId());
        if (recommendationChanged)
        {
            clearDetailsOverlay();
            setWrappedText(feedbackStatus, "", TEXT_WIDTH);
        }
        currentRecommendation = best;
        if (recommendationChanged)
            riskAcknowledgedRecommendationId = null;
        updateDetailsButtonLabel();

        setRecommendationButtonsEnabled(
                !FallbackRecommendationFactory.isFallback(best));
        setWrappedText(recommendationTitle, safe(best.getTitle()), TEXT_WIDTH);

        Skill skill = MilestoneTracker.skillFor(best);
        if (recommendationChanged)
        {
            if (skillIconLoader != null) skillIconLoader.clear(recommendationIcon);
            else recommendationIcon.setIcon(null);
            if (skill != null)
            {
                recommendationEyebrow.setText(skill.getName().toUpperCase());
                if (skillIconLoader != null)
                    skillIconLoader.load(skill, recommendationIcon, 26);
            }
            else
            {
                recommendationEyebrow.setText("NEXT MOVE");
            }
        }

        updateProgress(best);
        renderRecommendationBody();
        setWrappedText(alternativeOne,
                recommendations.size() > 1
                        ? alternativeText(recommendations.get(1))
                        : "",
                TEXT_WIDTH);
        setWrappedText(alternativeTwo,
                recommendations.size() > 2
                        ? alternativeText(recommendations.get(2))
                        : "",
                TEXT_WIDTH);
        alternativeTwo.setVisible(recommendations.size() > 2);
        alternativesCard.setVisible(recommendations.size() > 1);
        revalidate();
        repaint();
    }

    public void showMilestoneCompletion(MilestoneCompletion completion)
    {
        if (completion == null) return;

        milestoneTitle.setText(html("✓ Milestone complete"));
        setWrappedText(milestoneBody,
                completion.getSkill().getName()
                        + " "
                        + completion.getStartedAtLevel()
                        + " → "
                        + completion.getTargetLevel()
                        + ". Picking your next move...",
                TEXT_WIDTH);
        milestoneBanner.setVisible(true);

        if (milestoneHideTimer != null) milestoneHideTimer.stop();
        milestoneHideTimer = new Timer(8000, event ->
        {
            milestoneBanner.setVisible(false);
            revalidate();
            repaint();
        });
        milestoneHideTimer.setRepeats(false);
        milestoneHideTimer.start();
        revalidate();
        repaint();
    }

    public void updateOpportunities(List<Opportunity> opportunities)
    {
        if (opportunities == null || opportunities.isEmpty())
        {
            setWrappedText(opportunityOne, "", TEXT_WIDTH);
            setWrappedText(opportunityTwo, "", TEXT_WIDTH);
            opportunitiesCard.setVisible(false);
            return;
        }

        setWrappedText(opportunityOne, opportunityText(opportunities.get(0)), TEXT_WIDTH);
        setWrappedText(opportunityTwo,
                opportunities.size() > 1 ? opportunityText(opportunities.get(1)) : "",
                TEXT_WIDTH);
        opportunityTwo.setVisible(opportunities.size() > 1);
        opportunitiesCard.setVisible(true);
        revalidate();
        repaint();
    }

    private void updateProgress(Recommendation recommendation)
    {
        int current = recommendation.getCurrentLevel();
        int target = recommendation.getCurrentExecutionTargetLevel();
        if (current <= 0 || target <= current)
        {
            progressText.setText(html(""));
            progressBar.setValue(0);
            progressText.setVisible(false);
            progressBar.setVisible(false);
            return;
        }

        int start = target <= 10
                ? 1
                : target == 99
                ? 90
                : Math.max(1, target - 10);
        int percent = Math.max(
                0,
                Math.min(
                        100,
                        (int) Math.round(
                                Math.max(0, current - start)
                                        * 100.0
                                        / Math.max(1, target - start))));
        progressText.setText(html("Level " + current + " → " + target));
        progressBar.setValue(percent);
        progressText.setVisible(true);
        progressBar.setVisible(true);
    }

    private void toggleDetails()
    {
        if (currentRecommendation == null || !detailsOverlayEnabled) return;
        acknowledgeFirstUse();
        if (requiresRiskAcknowledgement(currentRecommendation)
                && !currentRecommendation.getId().equals(
                        riskAcknowledgedRecommendationId))
            riskAcknowledgedRecommendationId = currentRecommendation.getId();
        detailsVisible = !detailsVisible;
        updateDetailsButtonLabel();
        detailsHandler.accept(detailsVisible ? currentRecommendation : null);
    }

    private void clearDetailsOverlay()
    {
        if (detailsVisible) detailsHandler.accept(null);
        detailsVisible = false;
        updateDetailsButtonLabel();
    }

    private void updateDetailsButtonLabel()
    {
        if (requiresRiskAcknowledgement(currentRecommendation))
            detailsButton.setText(detailsVisible
                    ? "Hide Risk Steps" : "View Risk Steps");
        else
            detailsButton.setText(detailsVisible ? "Hide Details" : "Details");
    }

    private static boolean requiresRiskAcknowledgement(
            Recommendation recommendation)
    {
        if (recommendation == null || recommendation.getGuidance() == null
                || recommendation.getGuidance().getRiskDisclosure() == null)
            return false;
        return recommendation.getGuidance().getRiskDisclosure()
                .isAcknowledgementRequired();
    }

    private void renderRecommendationBody()
    {
        if (currentRecommendation == null)
        {
            setWrappedText(recommendationBody, "", TEXT_WIDTH);
            return;
        }
        setWrappedText(
                recommendationBody,
                RecommendationPresentation.compactText(currentRecommendation,
                        currentGoalContext()),
                TEXT_WIDTH);
    }

    private void submitFeedback(FeedbackAction action)
    {
        if (currentRecommendation == null || feedbackHandler == null) return;

        String title = currentRecommendation.getTitle();
        String id = currentRecommendation.getId();
        setWrappedText(feedbackStatus, feedbackStatusText(action, title), TEXT_WIDTH);
        acknowledgeFirstUse();
        feedbackHandler.accept(id, action);
    }

    private void setRecommendationButtonsEnabled(boolean enabled)
    {
        detailsButton.setEnabled(enabled && detailsOverlayEnabled);
        detailsButton.setVisible(enabled && detailsOverlayEnabled);
        laterButton.setEnabled(enabled);
        notTodayButton.setEnabled(enabled);
        dislikeButton.setEnabled(enabled);
        feedbackPanel.setVisible(enabled);
        recommendationControls.setVisible(enabled);
    }

    public void setDetailsOverlayEnabled(boolean enabled)
    {
        detailsOverlayEnabled = enabled;
        if (!enabled) clearDetailsOverlay();
        setRecommendationButtonsEnabled(currentRecommendation != null
                && !FallbackRecommendationFactory.isFallback(currentRecommendation));
        revalidate();
        repaint();
    }

    public void closeDetails() { clearDetailsOverlay(); }

    public void setFirstUseHintVisible(boolean visible)
    {
        firstUseHint.setVisible(visible);
        revalidate();
        repaint();
    }

    boolean isSupportVisible() { return supportButton.isVisible(); }
    boolean isFirstUseHintVisible() { return firstUseHint.isVisible(); }
    boolean isDetailsControlEnabled() { return detailsButton.isEnabled(); }
    boolean isDetailsControlVisible() { return detailsButton.isVisible(); }
    boolean isFeedbackVisibleForTest() { return feedbackPanel.isVisible(); }
    boolean isProgressVisibleForTest() { return progressBar.isVisible(); }
    String detailsLabelForTest() { return detailsButton.getText(); }
    void clickDetailsForTest() { detailsButton.doClick(); }
    void clickSupportForTest() { supportButton.doClick(); }
    String recommendationTextForTest() { return recommendationBody.getText(); }
    int recommendationTextHeightForTest()
    {
        return recommendationBody.getPreferredSize().height;
    }
    int recommendationTextLineHeightForTest()
    {
        return recommendationBody.getFontMetrics(
                recommendationBody.getFont()).getHeight();
    }
    int recommendationTextLineCountForTest()
    {
        return recommendationBody.getLineCount();
    }
    boolean areAlternativesVisibleForTest() { return alternativesCard.isVisible(); }
    boolean areOpportunitiesVisibleForTest() { return opportunitiesCard.isVisible(); }
    String firstAlternativeTextForTest() { return alternativeOne.getText(); }
    String firstOpportunityTextForTest() { return opportunityOne.getText(); }
    java.util.List<String> feedbackLabelsForTest()
    {
        return java.util.Arrays.asList(
                laterButton.getText(), notTodayButton.getText(), dislikeButton.getText());
    }

    private GoalRecommendationContext currentGoalContext()
    {
        return GoalRecommendationContext.assess(
                selectedGoal, currentRecommendation, membership);
    }

    private void acknowledgeFirstUse()
    {
        firstUseHint.setVisible(false);
        firstUseHandler.run();
    }

    private static MembershipStatus membershipFromDisplay(String display)
    {
        if (display == null) return MembershipStatus.UNKNOWN;
        for (MembershipStatus status : MembershipStatus.values())
            if (status.getDisplayName().equalsIgnoreCase(display.trim()))
                return status;
        return MembershipStatus.UNKNOWN;
    }

    static String alternativeText(Recommendation recommendation)
    {
        if (recommendation == null) return "";
        TrainingPlan plan = recommendation.getTrainingPlan();
        if (plan != null && plan.getMethod() != null
                && plan.getMethod().getSkill() != null)
        {
            StringBuilder value = new StringBuilder()
                    .append(plan.getMethod().getSkill().getName());
            if (recommendation.getCurrentLevel() > 0
                    && recommendation.getTargetLevel()
                            > recommendation.getCurrentLevel())
                value.append(' ').append(recommendation.getCurrentLevel())
                        .append(" → ").append(recommendation.getTargetLevel());
            value.append('\n').append(RecommendationPresentation.compactSentence(
                    plan.getMethod().getName(), 58));
            return value.toString();
        }
        return RecommendationPresentation.compactSentence(
                safe(recommendation.getTitle()), 82);
    }

    private static void makeFullWidth(JButton button, int height)
    {
        button.setAlignmentX(LEFT_ALIGNMENT);
        Dimension size = new Dimension(INNER_WIDTH, height);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.setMinimumSize(size);
    }

    private static JButton actionButton(String text)
    {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setFont(button.getFont().deriveFont(BUTTON_FONT_SIZE));
        button.setMargin(new Insets(4, 6, 4, 6));
        button.setBackground(StrategistTheme.CARD_HOVER);
        button.setForeground(StrategistTheme.TEXT);
        return button;
    }

    private static JPanel cardPanel(boolean highlighted)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(StrategistTheme.CARD);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setBorder(highlighted
                ? StrategistTheme.highlightedCardBorder()
                : StrategistTheme.cardBorder());
        panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
        return panel;
    }

    private static JTextArea wrappingArea(
            String text,
            float fontSize,
            Color foreground,
            boolean bold)
    {
        JTextArea area = new JTextArea(text == null ? "" : text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        area.setMargin(new Insets(0, 0, 0, 0));
        area.setForeground(foreground);
        area.setFont(area.getFont().deriveFont(
                bold ? Font.BOLD : Font.PLAIN,
                fontSize));
        area.setAlignmentX(LEFT_ALIGNMENT);
        return area;
    }

    /**
     * Force Swing to lay the text out at the real card width before BoxLayout
     * asks for dimensions. This keeps complete words inside the sidebar rather
     * than relying on JLabel HTML width hints.
     */
    private static void setWrappedText(JTextArea area, String text, int width)
    {
        if (area == null) return;
        String value = text == null ? "" : text;
        area.setText(value);
        // setPreferredSize makes Swing cache the previous measurement. Clear
        // all three explicit bounds before measuring new copy, otherwise an
        // area initialized empty remains one line tall even after a resolved
        // multi-line recommendation replaces it.
        area.setPreferredSize(null);
        area.setMinimumSize(null);
        area.setMaximumSize(null);
        area.setSize(new Dimension(width, 10_000));
        Dimension preferred = area.getPreferredSize();
        int lineHeight = area.getFontMetrics(area.getFont()).getHeight();
        int height = value.isEmpty() ? 1 : Math.max(lineHeight, preferred.height);
        Dimension size = new Dimension(width, height);
        area.setPreferredSize(size);
        area.setMinimumSize(size);
        area.setMaximumSize(size);
    }

    private static JLabel eyebrow(String text)
    {
        JLabel label = mutedLabel(text);
        label.setForeground(StrategistTheme.GOLD_SOFT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, EYEBROW_FONT_SIZE));
        return label;
    }

    private static JLabel label(String text)
    {
        JLabel label = new JLabel(html(text));
        label.setForeground(StrategistTheme.TEXT);
        label.setFont(label.getFont().deriveFont(BODY_FONT_SIZE));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel mutedLabel(String text)
    {
        JLabel label = label(text);
        label.setForeground(StrategistTheme.MUTED_TEXT);
        label.setFont(label.getFont().deriveFont(MUTED_FONT_SIZE));
        return label;
    }

    private static JLabel inlineMutedLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(StrategistTheme.MUTED_TEXT);
        label.setFont(label.getFont().deriveFont(MUTED_FONT_SIZE));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static String opportunityText(Opportunity opportunity)
    {
        boolean actionable = opportunity.isReady()
                && (opportunity.isSetupVerified()
                || opportunity.getPreparation().isEmpty());
        String state;
        if (actionable)
            state = "Ready";
        else if (!opportunity.getPreparation().isEmpty())
            state = "Prep: " + opportunity.getPreparation().get(0);
        else
            state = "Wait";
        return "• " + safe(opportunity.getTitle()) + "\n" + state;
    }

    private static String feedbackStatusText(FeedbackAction action, String title)
    {
        String activity = safe(title == null ? "Recommendation" : title);
        switch (action)
        {
            case LATER:
                return activity + "\nLater for 1 hour";
            case NOT_TODAY:
                return activity + "\nHidden for today";
            case DISLIKE:
                return activity + "\nShowing less often";
            default:
                return "Feedback saved";
        }
    }

    private static String prettyName(String text)
    {
        if (text == null || text.isEmpty()) return "Unknown";
        String lower = text.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String safe(String value)
    {
        return value == null ? "" : value;
    }

    private static String escape(String value)
    {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String html(String text)
    {
        return "<html><div style='width:" + INNER_WIDTH + "px;'>"
                + (text == null ? "" : text)
                + "</div></html>";
    }
}
