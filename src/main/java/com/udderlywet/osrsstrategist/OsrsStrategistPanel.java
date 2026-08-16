package com.udderlywet.osrsstrategist;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import net.runelite.api.Skill;
import net.runelite.client.ui.PluginPanel;

/**
 * RuneLite sidebar for Strategist.
 *
 * <p>The default RuneLite plugin panel is intentionally narrow. Strategist has
 * more information to communicate than a typical toggle/settings panel, so this
 * panel deliberately asks RuneLite for a modestly wider sidebar. The extra room
 * is spent on legibility rather than on showing more data: larger body text,
 * fewer clipped lines, and clearer spacing between decisions.</p>
 *
 * <p>Deep reasoning remains behind {@code Details}. The collapsed card should
 * answer only three questions at a glance: what should I do, what method should
 * I use, and what do I need to check before starting?</p>
 */
public class OsrsStrategistPanel extends PluginPanel
{
    /**
     * RuneLite's normal PluginPanel is 225 px. 280 px is still a compact sidebar
     * but gives wrapped recommendation text enough room to remain readable on
     * normal desktop scaling. The wrapper includes RuneLite's scrollbar width.
     */
    private static final int STRATEGIST_PANEL_WIDTH = 280;
    private static final int WRAPPED_PANEL_WIDTH =
            STRATEGIST_PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH;

    // Widths below account for PluginPanel padding, our content padding, card
    // borders, and the recommendation icon. Keeping separate widths prevents an
    // HTML label from believing it has more horizontal space than Swing gives it.
    private static final int CONTENT_WIDTH = STRATEGIST_PANEL_WIDTH - 28;
    private static final int INNER_WIDTH = CONTENT_WIDTH - 20;
    private static final int BODY_TEXT_WIDTH = INNER_WIDTH - 10;
    private static final int TITLE_TEXT_WIDTH = INNER_WIDTH - 38;

    private static final float BODY_FONT_SIZE = 12.5f;
    private static final float SMALL_FONT_SIZE = 11.0f;
    private static final float RECOMMENDATION_TITLE_SIZE = 14.0f;

    private final SkillIconLoader skillIconLoader;
    private final BiConsumer<String, FeedbackAction> feedbackHandler;

    private final JLabel accountName = label("Waiting for login...");
    private final JLabel accountMeta = mutedLabel("Unknown • -- / 2376");

    private final JLabel activeGoal = label("Goal: Max");
    private final JLabel strategySummary = mutedLabel(
            "Balanced • Pick for me • Normal quests"
    );

    private final JPanel milestoneBanner = cardPanel(true);
    private final JLabel milestoneTitle = label("Milestone complete!");
    private final JLabel milestoneBody = mutedLabel("");
    private Timer milestoneHideTimer;

    private final JPanel recommendationCard = cardPanel(true);
    private final JLabel recommendationIcon = new JLabel();
    private final JLabel recommendationEyebrow = mutedLabel("NEXT MOVE");
    private final JLabel recommendationTitle = label("Analyzing account...");
    private final JLabel progressText = mutedLabel("");
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel recommendationBody = label("");
    private final JLabel feedbackStatus = mutedLabel("");

    private final JLabel alternativeOne = label("");
    private final JLabel alternativeTwo = label("");

    private final JLabel opportunityOne = label("No active reminders yet.");
    private final JLabel opportunityTwo = label("");

    private final JButton detailsButton = actionButton("Details");
    private final JButton laterButton = actionButton("Later");
    private final JButton notTodayButton = actionButton("Not Today");
    private final JButton dislikeButton = actionButton("Dislike");

    private Recommendation currentRecommendation;
    private boolean detailsVisible;

    public OsrsStrategistPanel(
            BiConsumer<String, FeedbackAction> feedbackHandler,
            SkillIconLoader skillIconLoader)
    {
        this.feedbackHandler = feedbackHandler;
        this.skillIconLoader = skillIconLoader;

        configurePanelWidth();
        setLayout(new BorderLayout());
        setBackground(StrategistTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(StrategistTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(9, 8, 14, 8));

        buildHeader(content);
        buildStrategyCard(content);
        buildMilestoneBanner(content);
        buildRecommendationCard(content);
        buildSecondaryCards(content);

        add(content, BorderLayout.NORTH);
    }

    /**
     * PluginPanel wraps itself in a fixed-width panel by default. Updating both
     * the inner panel and wrapper is required for RuneLite's ClientUI layout to
     * allocate the wider sidebar instead of simply clipping our content.
     */
    private void configurePanelWidth()
    {
        getWrappedPanel().setPreferredSize(
                new Dimension(WRAPPED_PANEL_WIDTH, 0));
        getWrappedPanel().setMinimumSize(
                new Dimension(WRAPPED_PANEL_WIDTH, 0));
        if (getScrollPane() != null)
        {
            getScrollPane().setPreferredSize(
                    new Dimension(WRAPPED_PANEL_WIDTH, 0));
            getScrollPane().setMinimumSize(
                    new Dimension(WRAPPED_PANEL_WIDTH, 0));
        }
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(
                STRATEGIST_PANEL_WIDTH,
                super.getPreferredSize().height);
    }

    @Override
    public Dimension getMinimumSize()
    {
        return new Dimension(
                STRATEGIST_PANEL_WIDTH,
                super.getMinimumSize().height);
    }

    private void buildHeader(JPanel content)
    {
        JLabel title = label("OSRS STRATEGIST");
        title.setForeground(StrategistTheme.GOLD);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel subtitle = mutedLabel("Adaptive progression planner");

        content.add(title);
        content.add(Box.createVerticalStrut(3));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(11));

        JPanel accountCard = cardPanel(false);
        accountName.setFont(accountName.getFont().deriveFont(Font.BOLD, 13.5f));
        accountCard.add(accountName);
        accountCard.add(Box.createVerticalStrut(3));
        accountCard.add(accountMeta);

        content.add(accountCard);
        content.add(Box.createVerticalStrut(9));
    }

    private void buildStrategyCard(JPanel content)
    {
        JPanel strategyCard = cardPanel(false);

        JLabel section = eyebrow("STRATEGY");
        activeGoal.setForeground(StrategistTheme.GOLD);
        activeGoal.setFont(activeGoal.getFont().deriveFont(Font.BOLD, 13f));

        strategyCard.add(section);
        strategyCard.add(Box.createVerticalStrut(5));
        strategyCard.add(activeGoal);
        strategyCard.add(Box.createVerticalStrut(3));
        strategyCard.add(strategySummary);

        content.add(strategyCard);
        content.add(Box.createVerticalStrut(9));
    }

    private void buildMilestoneBanner(JPanel content)
    {
        milestoneBanner.setBackground(StrategistTheme.CARD);
        milestoneTitle.setForeground(StrategistTheme.SUCCESS);
        milestoneTitle.setFont(
                milestoneTitle.getFont().deriveFont(Font.BOLD, 13.5f)
        );

        milestoneBanner.add(milestoneTitle);
        milestoneBanner.add(Box.createVerticalStrut(4));
        milestoneBanner.add(milestoneBody);
        milestoneBanner.setVisible(false);

        content.add(milestoneBanner);
        content.add(Box.createVerticalStrut(9));
    }

    private void buildRecommendationCard(JPanel content)
    {
        content.add(eyebrow("DO NEXT"));
        content.add(Box.createVerticalStrut(6));

        JPanel topRow = new JPanel(new BorderLayout(9, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(INNER_WIDTH, 72));

        recommendationIcon.setHorizontalAlignment(SwingConstants.CENTER);
        recommendationIcon.setVerticalAlignment(SwingConstants.CENTER);
        recommendationIcon.setPreferredSize(new Dimension(29, 29));
        recommendationIcon.setMinimumSize(new Dimension(29, 29));

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setMaximumSize(new Dimension(TITLE_TEXT_WIDTH, 72));

        recommendationEyebrow.setFont(
                recommendationEyebrow.getFont().deriveFont(Font.BOLD, 10f)
        );
        recommendationTitle.setFont(
                recommendationTitle.getFont().deriveFont(
                        Font.BOLD, RECOMMENDATION_TITLE_SIZE)
        );
        titleStack.add(recommendationEyebrow);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(recommendationTitle);

        topRow.add(recommendationIcon, BorderLayout.WEST);
        topRow.add(titleStack, BorderLayout.CENTER);

        recommendationCard.add(topRow);
        recommendationCard.add(Box.createVerticalStrut(8));

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
        recommendationCard.add(Box.createVerticalStrut(10));
        recommendationCard.add(recommendationBody);
        recommendationCard.add(Box.createVerticalStrut(10));

        detailsButton.setAlignmentX(LEFT_ALIGNMENT);
        detailsButton.setMaximumSize(new Dimension(INNER_WIDTH, 30));
        detailsButton.setPreferredSize(new Dimension(INNER_WIDTH, 30));
        recommendationCard.add(detailsButton);
        recommendationCard.add(Box.createVerticalStrut(8));

        JPanel feedbackPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        feedbackPanel.setOpaque(false);
        feedbackPanel.setAlignmentX(LEFT_ALIGNMENT);
        feedbackPanel.setPreferredSize(new Dimension(INNER_WIDTH, 31));
        feedbackPanel.setMaximumSize(new Dimension(INNER_WIDTH, 31));
        feedbackPanel.add(laterButton);
        feedbackPanel.add(notTodayButton);
        feedbackPanel.add(dislikeButton);

        recommendationCard.add(feedbackPanel);
        recommendationCard.add(Box.createVerticalStrut(6));
        recommendationCard.add(feedbackStatus);

        detailsButton.addActionListener(event -> toggleDetails());
        laterButton.addActionListener(
                event -> submitFeedback(FeedbackAction.LATER)
        );
        notTodayButton.addActionListener(
                event -> submitFeedback(FeedbackAction.NOT_TODAY)
        );
        dislikeButton.addActionListener(
                event -> submitFeedback(FeedbackAction.DISLIKE)
        );

        setRecommendationButtonsEnabled(false);

        content.add(recommendationCard);
        content.add(Box.createVerticalStrut(11));
    }

    private void buildSecondaryCards(JPanel content)
    {
        JPanel alternativesCard = cardPanel(false);
        alternativesCard.add(eyebrow("OTHER GOOD OPTIONS"));
        alternativesCard.add(Box.createVerticalStrut(6));
        alternativesCard.add(alternativeOne);
        alternativesCard.add(Box.createVerticalStrut(5));
        alternativesCard.add(alternativeTwo);

        content.add(alternativesCard);
        content.add(Box.createVerticalStrut(9));

        JPanel opportunitiesCard = cardPanel(false);
        opportunitiesCard.add(eyebrow("OPPORTUNITIES"));
        opportunitiesCard.add(Box.createVerticalStrut(6));
        opportunitiesCard.add(opportunityOne);
        opportunitiesCard.add(Box.createVerticalStrut(5));
        opportunitiesCard.add(opportunityTwo);

        content.add(opportunitiesCard);
    }

    /** Compatibility overload for logged-out/older callers. */
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
        accountName.setText(html(escape(name)));
        accountMeta.setText(html(
                escape(type)
                        + " • "
                        + escape(membership)
                        + " • "
                        + (total > 0 ? total : "--")
                        + " / 2376"));
    }

    public void updateGoal(GoalType goal)
    {
        GoalType safeGoal = goal == null ? GoalType.MAX : goal;
        activeGoal.setText(html("Goal: " + prettyName(safeGoal.name())));
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
        StrategyMode safeMode = mode == null ? StrategyMode.BALANCED : mode;
        SessionIntent safeIntent = intent == null
                ? SessionIntent.PICK_FOR_ME : intent;
        QuestTolerance safeTolerance = tolerance == null
                ? QuestTolerance.NORMAL : tolerance;
        strategySummary.setText(html(
                prettyName(safeMode.name())
                        + " • "
                        + prettyName(safeIntent.name())
                        + " • "
                        + prettyName(safeTolerance.name())
                        + " quests"));
    }

    public void updateRecommendations(List<Recommendation> recommendations)
    {
        if (recommendations == null || recommendations.isEmpty())
        {
            currentRecommendation = null;
            detailsVisible = false;
            detailsButton.setText("Details");
            setRecommendationButtonsEnabled(false);

            recommendationIcon.setIcon(null);
            recommendationEyebrow.setText(html("NEXT MOVE"));
            recommendationTitle.setText(titleHtml("No recommendation available"));
            progressText.setText(html(""));
            progressBar.setValue(0);
            recommendationBody.setText(bodyHtml(""));
            feedbackStatus.setText(bodyHtml(""));
            alternativeOne.setText(html(""));
            alternativeTwo.setText(html(""));
            return;
        }

        Recommendation best = recommendations.get(0);
        String previousId = currentRecommendation == null
                ? null : currentRecommendation.getId();
        currentRecommendation = best;

        // A rotated recommendation starts collapsed. A routine stat refresh of
        // the same recommendation preserves the player's chosen Details state.
        if (previousId == null || !previousId.equals(best.getId()))
        {
            detailsVisible = false;
            detailsButton.setText("Details");
            feedbackStatus.setText(bodyHtml(""));
        }

        setRecommendationButtonsEnabled(true);
        recommendationTitle.setText(titleHtml(escape(best.getTitle())));

        Skill skill = MilestoneTracker.skillFor(best);
        recommendationIcon.setIcon(null);
        if (skill != null)
        {
            recommendationEyebrow.setText(
                    html(escape(skill.getName().toUpperCase())));
            skillIconLoader.load(skill, recommendationIcon, 25);
        }
        else
        {
            recommendationEyebrow.setText(html("NEXT MOVE"));
        }

        updateProgress(best);
        renderRecommendationBody();

        alternativeOne.setText(html(
                recommendations.size() > 1
                        ? "• " + escape(recommendations.get(1).getTitle())
                        : ""));
        alternativeTwo.setText(html(
                recommendations.size() > 2
                        ? "• " + escape(recommendations.get(2).getTitle())
                        : ""));
    }

    /**
     * Displays a short, non-modal celebration. It disappears automatically so
     * the useful planning content remains the visual focus.
     */
    public void showMilestoneCompletion(MilestoneCompletion completion)
    {
        if (completion == null) return;

        milestoneTitle.setText(html("✓ Milestone complete"));
        milestoneBody.setText(bodyHtml(
                escape(completion.getSkill().getName())
                        + " "
                        + completion.getStartedAtLevel()
                        + " → "
                        + completion.getTargetLevel()
                        + ". Picking your next move..."));
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
            opportunityOne.setText(html("No active reminders yet."));
            opportunityTwo.setText(html(""));
            return;
        }

        opportunityOne.setText(html(opportunityText(opportunities.get(0))));
        opportunityTwo.setText(html(
                opportunities.size() > 1
                        ? opportunityText(opportunities.get(1))
                        : ""));
    }

    private void updateProgress(Recommendation recommendation)
    {
        int current = recommendation.getCurrentLevel();
        int target = recommendation.getTargetLevel();

        if (current <= 0 || target <= current)
        {
            progressText.setText(html(""));
            progressBar.setValue(0);
            return;
        }

        int start = checkpointStart(target);
        int range = Math.max(1, target - start);
        int progressed = Math.max(0, current - start);
        int percent = Math.max(0, Math.min(100,
                (int) Math.round(progressed * 100.0 / range)));

        progressText.setText(html("Level " + current + " → " + target));
        progressBar.setValue(percent);
    }

    private static int checkpointStart(int target)
    {
        if (target <= 10) return 1;
        if (target == 99) return 90;
        return Math.max(1, target - 10);
    }

    private void toggleDetails()
    {
        if (currentRecommendation == null) return;

        detailsVisible = !detailsVisible;
        detailsButton.setText(detailsVisible ? "Hide Details" : "Details");
        renderRecommendationBody();
        revalidate();
        repaint();
    }

    private void renderRecommendationBody()
    {
        if (currentRecommendation == null)
        {
            recommendationBody.setText(bodyHtml(""));
            return;
        }

        String body = detailsVisible
                ? RecommendationPresentation.detailedHtml(currentRecommendation)
                : RecommendationPresentation.compactHtml(currentRecommendation);
        recommendationBody.setText(bodyHtml(body));
    }

    private void submitFeedback(FeedbackAction action)
    {
        if (currentRecommendation == null || feedbackHandler == null) return;

        String actedOnTitle = currentRecommendation.getTitle();
        String actedOnId = currentRecommendation.getId();
        feedbackStatus.setText(bodyHtml(
                feedbackStatusText(action, actedOnTitle)));
        feedbackHandler.accept(actedOnId, action);
    }

    private void setRecommendationButtonsEnabled(boolean enabled)
    {
        detailsButton.setEnabled(enabled);
        laterButton.setEnabled(enabled);
        notTodayButton.setEnabled(enabled);
        dislikeButton.setEnabled(enabled);
    }

    private static JButton actionButton(String text)
    {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setFont(button.getFont().deriveFont(SMALL_FONT_SIZE));
        button.setMargin(new Insets(3, 3, 3, 3));
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

    private static JLabel eyebrow(String text)
    {
        JLabel label = mutedLabel(text);
        label.setForeground(StrategistTheme.GOLD_SOFT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10.5f));
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
        return label;
    }

    private static String opportunityText(Opportunity opportunity)
    {
        String state = opportunity.isReady()
                ? "Ready"
                : confidenceName(opportunity.getConfidence());
        return "• <b>" + escape(opportunity.getTitle())
                + "</b> • " + state;
    }

    private static String feedbackStatusText(
            FeedbackAction action,
            String title)
    {
        String activity = escape(title == null ? "Recommendation" : title);
        switch (action)
        {
            case DO_THIS:
                return "Selected: " + activity;
            case LATER:
                return activity + " • later (1h)";
            case NOT_TODAY:
                return activity + " • hidden today";
            case DISLIKE:
                return activity + " • showing less often";
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

    private static String confidenceName(RecommendationConfidence confidence)
    {
        if (confidence == RecommendationConfidence.VERIFIED) return "Ready";
        if (confidence == RecommendationConfidence.BLOCKED) return "Blocked";
        return "Check";
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
        return htmlWithWidth(text, INNER_WIDTH);
    }

    private static String titleHtml(String text)
    {
        return htmlWithWidth(text, TITLE_TEXT_WIDTH);
    }

    private static String bodyHtml(String text)
    {
        return htmlWithWidth(text, BODY_TEXT_WIDTH);
    }

    private static String htmlWithWidth(String text, int width)
    {
        return "<html><div style='width:"
                + width
                + "px;'>"
                + text
                + "</div></html>";
    }
}
