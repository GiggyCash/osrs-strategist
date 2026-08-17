package com.udderlywet.osrsstrategist;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
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

/** RuneLite sidebar for Strategist. Designed to use vertical space, not width. */
public class OsrsStrategistPanel extends PluginPanel
{
    private static final int CONTENT_PADDING = 8;
    private static final int CARD_HORIZONTAL_INSET = 20;

    /*
     * PluginPanel already reserves BORDER_OFFSET on both sides. Our content adds
     * another 8px per side, and StrategistTheme cards use 10px per side. Keep
     * every fixed-width child inside those real bounds so Swing never paints
     * text or controls beneath the right edge of the RuneLite sidebar.
     */
    private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH
            - (PluginPanel.BORDER_OFFSET * 2)
            - (CONTENT_PADDING * 2);
    private static final int INNER_WIDTH = CONTENT_WIDTH - CARD_HORIZONTAL_INSET;
    private static final int BODY_TEXT_WIDTH = INNER_WIDTH - 6;

    private final SkillIconLoader skillIconLoader;
    private final BiConsumer<String, FeedbackAction> feedbackHandler;

    private final JLabel accountName = label("Waiting for login...");
    private final JLabel accountMeta = mutedLabel("Unknown • -- / 2376");
    private final JLabel activeGoal = label("Goal: Max");
    private final JLabel strategySummary = mutedLabel(
            "Mode: Balanced<br>Session: Pick for me<br>Quests: Normal"
    );

    private final JPanel milestoneBanner = cardPanel(true);
    private final JLabel milestoneTitle = label("Milestone complete!");
    private final JLabel milestoneBody = mutedLabel("");
    private Timer milestoneHideTimer;

    private final JPanel recommendationCard = cardPanel(true);
    private final JLabel recommendationIcon = new JLabel();
    private final JLabel recommendationEyebrow = inlineMutedLabel("NEXT MOVE");
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
        setLayout(new BorderLayout());
        setBackground(StrategistTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(StrategistTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(
                8,
                CONTENT_PADDING,
                12,
                CONTENT_PADDING
        ));
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
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        content.add(title);
        content.add(Box.createVerticalStrut(3));

        JLabel subtitle = mutedLabel("Adaptive progression planner");
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(10));

        JPanel accountCard = cardPanel(false);
        accountName.setFont(accountName.getFont().deriveFont(Font.BOLD, 13f));
        accountCard.add(accountName);
        accountCard.add(Box.createVerticalStrut(4));
        accountCard.add(accountMeta);
        content.add(accountCard);
        content.add(Box.createVerticalStrut(8));
    }

    private void buildStrategyCard(JPanel content)
    {
        JPanel card = cardPanel(false);
        activeGoal.setForeground(StrategistTheme.GOLD);
        activeGoal.setFont(activeGoal.getFont().deriveFont(Font.BOLD, 13f));
        card.add(eyebrow("STRATEGY"));
        card.add(Box.createVerticalStrut(6));
        card.add(activeGoal);
        card.add(Box.createVerticalStrut(6));
        card.add(strategySummary);
        content.add(card);
        content.add(Box.createVerticalStrut(8));
    }

    private void buildMilestoneBanner(JPanel content)
    {
        milestoneTitle.setForeground(StrategistTheme.SUCCESS);
        milestoneTitle.setFont(milestoneTitle.getFont().deriveFont(Font.BOLD, 13f));
        milestoneBanner.add(milestoneTitle);
        milestoneBanner.add(Box.createVerticalStrut(4));
        milestoneBanner.add(milestoneBody);
        milestoneBanner.setVisible(false);
        content.add(milestoneBanner);
        content.add(Box.createVerticalStrut(8));
    }

    private void buildRecommendationCard(JPanel content)
    {
        content.add(eyebrow("DO NEXT"));
        content.add(Box.createVerticalStrut(5));

        JPanel identityRow = new JPanel(new BorderLayout(7, 0));
        identityRow.setOpaque(false);
        identityRow.setAlignmentX(LEFT_ALIGNMENT);
        identityRow.setMaximumSize(new Dimension(INNER_WIDTH, 30));

        recommendationIcon.setHorizontalAlignment(SwingConstants.CENTER);
        recommendationIcon.setVerticalAlignment(SwingConstants.CENTER);
        recommendationIcon.setPreferredSize(new Dimension(24, 24));
        recommendationEyebrow.setFont(
                recommendationEyebrow.getFont().deriveFont(Font.BOLD, 10f)
        );
        identityRow.add(recommendationIcon, BorderLayout.WEST);
        identityRow.add(recommendationEyebrow, BorderLayout.CENTER);

        recommendationTitle.setFont(
                recommendationTitle.getFont().deriveFont(Font.BOLD, 13f)
        );
        recommendationCard.add(identityRow);
        recommendationCard.add(Box.createVerticalStrut(5));
        recommendationCard.add(recommendationTitle);
        recommendationCard.add(Box.createVerticalStrut(8));

        progressBar.setAlignmentX(LEFT_ALIGNMENT);
        progressBar.setPreferredSize(new Dimension(INNER_WIDTH, 7));
        progressBar.setMaximumSize(new Dimension(INNER_WIDTH, 7));
        progressBar.setMinimumSize(new Dimension(INNER_WIDTH, 7));
        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(false);
        progressBar.setBackground(StrategistTheme.DIVIDER);
        progressBar.setForeground(StrategistTheme.GOLD);
        recommendationCard.add(progressText);
        recommendationCard.add(Box.createVerticalStrut(3));
        recommendationCard.add(progressBar);
        recommendationCard.add(Box.createVerticalStrut(10));
        recommendationCard.add(recommendationBody);
        recommendationCard.add(Box.createVerticalStrut(11));

        makeFullWidth(detailsButton, 30);
        makeFullWidth(laterButton, 30);
        makeFullWidth(notTodayButton, 30);
        makeFullWidth(dislikeButton, 30);

        recommendationCard.add(detailsButton);
        recommendationCard.add(Box.createVerticalStrut(6));
        recommendationCard.add(laterButton);
        recommendationCard.add(Box.createVerticalStrut(5));
        recommendationCard.add(notTodayButton);
        recommendationCard.add(Box.createVerticalStrut(5));
        recommendationCard.add(dislikeButton);
        recommendationCard.add(Box.createVerticalStrut(7));
        recommendationCard.add(feedbackStatus);

        detailsButton.addActionListener(event -> toggleDetails());
        laterButton.addActionListener(event -> submitFeedback(FeedbackAction.LATER));
        notTodayButton.addActionListener(event -> submitFeedback(FeedbackAction.NOT_TODAY));
        dislikeButton.addActionListener(event -> submitFeedback(FeedbackAction.DISLIKE));
        setRecommendationButtonsEnabled(false);

        content.add(recommendationCard);
        content.add(Box.createVerticalStrut(10));
    }

    private void buildSecondaryCards(JPanel content)
    {
        JPanel alternatives = cardPanel(false);
        alternatives.add(eyebrow("OTHER GOOD OPTIONS"));
        alternatives.add(Box.createVerticalStrut(7));
        alternatives.add(alternativeOne);
        alternatives.add(Box.createVerticalStrut(8));
        alternatives.add(alternativeTwo);
        content.add(alternatives);
        content.add(Box.createVerticalStrut(8));

        JPanel opportunities = cardPanel(false);
        opportunities.add(eyebrow("OPPORTUNITIES"));
        opportunities.add(Box.createVerticalStrut(7));
        opportunities.add(opportunityOne);
        opportunities.add(Box.createVerticalStrut(8));
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
                        + " / 2376 total"
        ));
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
                        + "<br>Quests: " + prettyName(tolerance.name())
        ));
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
            recommendationEyebrow.setText("NEXT MOVE");
            recommendationTitle.setText(html("No recommendation available"));
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
                ? null
                : currentRecommendation.getId();
        currentRecommendation = best;

        if (previousId == null || !previousId.equals(best.getId()))
        {
            detailsVisible = false;
            detailsButton.setText("Details");
            feedbackStatus.setText(bodyHtml(""));
        }

        setRecommendationButtonsEnabled(true);
        recommendationTitle.setText(html(escape(best.getTitle())));

        Skill skill = MilestoneTracker.skillFor(best);
        recommendationIcon.setIcon(null);
        if (skill != null)
        {
            recommendationEyebrow.setText(skill.getName().toUpperCase());
            skillIconLoader.load(skill, recommendationIcon, 24);
        }
        else
        {
            recommendationEyebrow.setText("NEXT MOVE");
        }

        updateProgress(best);
        renderRecommendationBody();
        alternativeOne.setText(html(
                recommendations.size() > 1
                        ? "• " + escape(recommendations.get(1).getTitle())
                        : ""
        ));
        alternativeTwo.setText(html(
                recommendations.size() > 2
                        ? "• " + escape(recommendations.get(2).getTitle())
                        : ""
        ));
    }

    public void showMilestoneCompletion(MilestoneCompletion completion)
    {
        if (completion == null)
        {
            return;
        }

        milestoneTitle.setText(html("✓ Milestone complete"));
        milestoneBody.setText(bodyHtml(
                escape(completion.getSkill().getName())
                        + " "
                        + completion.getStartedAtLevel()
                        + " → "
                        + completion.getTargetLevel()
                        + ". Picking your next move..."
        ));
        milestoneBanner.setVisible(true);

        if (milestoneHideTimer != null)
        {
            milestoneHideTimer.stop();
        }

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
                        : ""
        ));
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
                                        / Math.max(1, target - start)
                        )
                )
        );
        progressText.setText(html("Level " + current + " → " + target));
        progressBar.setValue(percent);
    }

    private void toggleDetails()
    {
        if (currentRecommendation == null)
        {
            return;
        }

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
        if (currentRecommendation == null || feedbackHandler == null)
        {
            return;
        }

        String title = currentRecommendation.getTitle();
        String id = currentRecommendation.getId();
        feedbackStatus.setText(bodyHtml(feedbackStatusText(action, title)));
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
        button.setFont(button.getFont().deriveFont(11f));
        button.setMargin(new Insets(3, 5, 3, 5));
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
        panel.setBorder(
                highlighted
                        ? StrategistTheme.highlightedCardBorder()
                        : StrategistTheme.cardBorder()
        );
        panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
        return panel;
    }

    private static JLabel eyebrow(String text)
    {
        JLabel label = mutedLabel(text);
        label.setForeground(StrategistTheme.GOLD_SOFT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        return label;
    }

    private static JLabel label(String text)
    {
        JLabel label = new JLabel(html(text));
        label.setForeground(StrategistTheme.TEXT);
        label.setFont(label.getFont().deriveFont(12f));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel mutedLabel(String text)
    {
        JLabel label = label(text);
        label.setForeground(StrategistTheme.MUTED_TEXT);
        return label;
    }

    private static JLabel inlineMutedLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(StrategistTheme.MUTED_TEXT);
        label.setFont(label.getFont().deriveFont(11f));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static String opportunityText(Opportunity opportunity)
    {
        String state = opportunity.isReady()
                ? "Ready"
                : confidenceName(opportunity.getConfidence());
        return "• <b>" + escape(opportunity.getTitle()) + "</b><br>" + state;
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
                return activity + "<br>Later for 1 hour";
            case NOT_TODAY:
                return activity + "<br>Hidden for today";
            case DISLIKE:
                return activity + "<br>Showing less often";
            default:
                return "Feedback saved";
        }
    }

    private static String prettyName(String text)
    {
        String lower = text.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String confidenceName(RecommendationConfidence confidence)
    {
        if (confidence == RecommendationConfidence.VERIFIED)
        {
            return "Verified";
        }
        if (confidence == RecommendationConfidence.BLOCKED)
        {
            return "Blocked";
        }
        return "Check Needed";
    }

    private static String escape(String value)
    {
        if (value == null)
        {
            return "";
        }
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
