package com.udderlywet.osrsstrategist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
 * RuneLite sidebar for Strategist.
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

    private final JLabel accountName = label("Waiting for login...");
    private final JLabel accountMeta = mutedLabel("Unknown • -- / 2376");
    private final JLabel activeGoal = label("Goal: Max");
    private final JLabel strategySummary = mutedLabel(
            "Mode: Balanced<br>Session: Pick for me<br>Quests: Normal"
    );

    private final JPanel milestoneBanner = cardPanel(true);
    private final JLabel milestoneTitle = label("Milestone complete!");
    private final JTextArea milestoneBody = wrappingArea(
            "", MUTED_FONT_SIZE, StrategistTheme.MUTED_TEXT, false);
    private Timer milestoneHideTimer;

    private final JPanel recommendationCard = cardPanel(true);
    private final JLabel recommendationIcon = new JLabel();
    private final JLabel recommendationEyebrow = inlineMutedLabel("NEXT MOVE");
    private final JTextArea recommendationTitle = wrappingArea(
            "Analyzing account...", EMPHASIS_FONT_SIZE, StrategistTheme.TEXT, true);
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
            "No active reminders yet.", BODY_FONT_SIZE, StrategistTheme.TEXT, false);
    private final JTextArea opportunityTwo = wrappingArea(
            "", BODY_FONT_SIZE, StrategistTheme.TEXT, false);

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
        this(feedbackHandler, skillIconLoader, recommendation -> { });
    }

    public OsrsStrategistPanel(
            BiConsumer<String, FeedbackAction> feedbackHandler,
            SkillIconLoader skillIconLoader,
            Consumer<Recommendation> detailsHandler)
    {
        this.feedbackHandler = feedbackHandler;
        this.skillIconLoader = skillIconLoader;
        this.detailsHandler = detailsHandler == null
                ? recommendation -> { }
                : detailsHandler;

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
        add(content, BorderLayout.NORTH);
    }

    private void buildHeader(JPanel content)
    {
        JLabel title = label("OSRS STRATEGIST");
        title.setForeground(StrategistTheme.GOLD);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        content.add(title);
        content.add(Box.createVerticalStrut(4));

        JLabel subtitle = mutedLabel("Adaptive progression planner");
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
        makeFullWidth(laterButton, 34);
        makeFullWidth(notTodayButton, 34);
        makeFullWidth(dislikeButton, 34);

        recommendationCard.add(detailsButton);
        recommendationCard.add(Box.createVerticalStrut(7));
        recommendationCard.add(laterButton);
        recommendationCard.add(Box.createVerticalStrut(6));
        recommendationCard.add(notTodayButton);
        recommendationCard.add(Box.createVerticalStrut(6));
        recommendationCard.add(dislikeButton);
        recommendationCard.add(Box.createVerticalStrut(8));
        setWrappedText(feedbackStatus, "", TEXT_WIDTH);
        recommendationCard.add(feedbackStatus);

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
        JPanel alternatives = cardPanel(false);
        alternatives.add(eyebrow("OTHER GOOD OPTIONS"));
        alternatives.add(Box.createVerticalStrut(8));
        setWrappedText(alternativeOne, "", TEXT_WIDTH);
        alternatives.add(alternativeOne);
        alternatives.add(Box.createVerticalStrut(9));
        setWrappedText(alternativeTwo, "", TEXT_WIDTH);
        alternatives.add(alternativeTwo);
        content.add(alternatives);
        content.add(Box.createVerticalStrut(9));

        JPanel opportunities = cardPanel(false);
        opportunities.add(eyebrow("OPPORTUNITIES"));
        opportunities.add(Box.createVerticalStrut(8));
        setWrappedText(opportunityOne, "No active reminders yet.", TEXT_WIDTH);
        opportunities.add(opportunityOne);
        opportunities.add(Box.createVerticalStrut(9));
        setWrappedText(opportunityTwo, "", TEXT_WIDTH);
        opportunities.add(opportunityTwo);
        content.add(opportunities);
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
        accountName.setText(html(escape(name)));
        accountMeta.setText(html(
                escape(type)
                        + "<br>"
                        + escape(membership)
                        + "<br>"
                        + (total > 0 ? total : "--")
                        + " / 2376 total"));
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
        strategySummary.setText(html(
                "Mode: " + prettyName(mode.name())
                        + "<br>Session: " + prettyName(intent.name())
                        + "<br>Quests: " + prettyName(tolerance.name())));
    }

    public void updateRecommendations(List<Recommendation> recommendations)
    {
        if (recommendations == null || recommendations.isEmpty())
        {
            clearDetailsOverlay();
            currentRecommendation = null;
            setRecommendationButtonsEnabled(false);
            recommendationIcon.setIcon(null);
            recommendationEyebrow.setText("NEXT MOVE");
            setWrappedText(recommendationTitle, "No ready recommendation available", TEXT_WIDTH);
            progressText.setText(html(""));
            progressBar.setValue(0);
            setWrappedText(recommendationBody,
                    "More account evidence is needed. If you are logged in, open your inventory and equipment, then open the bank once; the next safe action will appear automatically when enough state is observed.",
                    TEXT_WIDTH);
            setWrappedText(feedbackStatus, "", TEXT_WIDTH);
            setWrappedText(alternativeOne, "", TEXT_WIDTH);
            setWrappedText(alternativeTwo, "", TEXT_WIDTH);
            revalidate();
            repaint();
            return;
        }

        Recommendation best = recommendations.get(0);
        String previousId = currentRecommendation == null
                ? null
                : currentRecommendation.getId();

        if (previousId == null || !previousId.equals(best.getId()))
        {
            clearDetailsOverlay();
            setWrappedText(feedbackStatus, "", TEXT_WIDTH);
        }
        currentRecommendation = best;

        setRecommendationButtonsEnabled(true);
        setWrappedText(recommendationTitle, safe(best.getTitle()), TEXT_WIDTH);

        Skill skill = MilestoneTracker.skillFor(best);
        recommendationIcon.setIcon(null);
        if (skill != null)
        {
            recommendationEyebrow.setText(skill.getName().toUpperCase());
            skillIconLoader.load(skill, recommendationIcon, 26);
        }
        else
        {
            recommendationEyebrow.setText("NEXT MOVE");
        }

        updateProgress(best);
        renderRecommendationBody();
        setWrappedText(alternativeOne,
                recommendations.size() > 1
                        ? "• " + safe(recommendations.get(1).getTitle())
                        : "",
                TEXT_WIDTH);
        setWrappedText(alternativeTwo,
                recommendations.size() > 2
                        ? "• " + safe(recommendations.get(2).getTitle())
                        : "",
                TEXT_WIDTH);
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
            setWrappedText(opportunityOne, "No active reminders yet.", TEXT_WIDTH);
            setWrappedText(opportunityTwo, "", TEXT_WIDTH);
            return;
        }

        setWrappedText(opportunityOne, opportunityText(opportunities.get(0)), TEXT_WIDTH);
        setWrappedText(opportunityTwo,
                opportunities.size() > 1 ? opportunityText(opportunities.get(1)) : "",
                TEXT_WIDTH);
        revalidate();
        repaint();
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
    }

    private void toggleDetails()
    {
        if (currentRecommendation == null) return;
        detailsVisible = !detailsVisible;
        detailsButton.setText(detailsVisible ? "Hide Details" : "Details");
        detailsHandler.accept(detailsVisible ? currentRecommendation : null);
    }

    private void clearDetailsOverlay()
    {
        if (detailsVisible) detailsHandler.accept(null);
        detailsVisible = false;
        detailsButton.setText("Details");
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
                RecommendationPresentation.compactText(currentRecommendation),
                TEXT_WIDTH);
    }

    private void submitFeedback(FeedbackAction action)
    {
        if (currentRecommendation == null || feedbackHandler == null) return;

        String title = currentRecommendation.getTitle();
        String id = currentRecommendation.getId();
        setWrappedText(feedbackStatus, feedbackStatusText(action, title), TEXT_WIDTH);
        feedbackHandler.accept(id, action);
    }

    private void setRecommendationButtonsEnabled(boolean enabled)
    {
        detailsButton.setEnabled(enabled);
        laterButton.setEnabled(enabled);
        notTodayButton.setEnabled(enabled);
        dislikeButton.setEnabled(enabled);
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
        String state = opportunity.isReady()
                ? "Ready"
                : confidenceName(opportunity.getConfidence());
        return "• " + safe(opportunity.getTitle()) + "\n" + state;
    }

    private static String feedbackStatusText(FeedbackAction action, String title)
    {
        String activity = safe(title == null ? "Recommendation" : title);
        switch (action)
        {
            case DO_THIS:
                return "Selected: " + activity;
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

    private static String confidenceName(RecommendationConfidence confidence)
    {
        if (confidence == RecommendationConfidence.VERIFIED) return "Verified";
        if (confidence == RecommendationConfidence.BLOCKED) return "Blocked";
        return "Check Needed";
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
