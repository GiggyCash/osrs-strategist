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
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * RuneLite sidebar for Strategist.
 *
 * <p>Important UX rule: the engine may have deep reasoning, but the default
 * panel must stay scannable. The concise recommendation is always visible;
 * deeper instructions and explanation live behind the Details button.</p>
 */
public class OsrsStrategistPanel extends PluginPanel
{
    private static final int CONTENT_WIDTH = 160;

    private final JLabel accountName = wrapLabel("Waiting for login...");
    private final JLabel accountType = wrapLabel("Account type: Unknown");
    private final JLabel totalLevel = wrapLabel("Total level: -- / 2376");

    private final JLabel strategyMode = wrapLabel("Mode: Balanced");
    private final JLabel sessionIntent = wrapLabel("Session: Pick for me");
    private final JLabel questTolerance = wrapLabel("Quest tolerance: Normal");

    private final JLabel recommendationTitle = wrapLabel("Analyzing account...");
    private final JLabel recommendationBody = wrapLabel("");
    private final JLabel feedbackStatus = wrapLabel("");

    private final JLabel alternativeOne = wrapLabel("");
    private final JLabel alternativeTwo = wrapLabel("");

    private final JLabel opportunityOne = wrapLabel("No active reminders yet.");
    private final JLabel opportunityTwo = wrapLabel("");

    private final JButton detailsButton = feedbackButton("Details");
    private final JButton doThisButton = feedbackButton("Do This");
    private final JButton laterButton = feedbackButton("Later");
    private final JButton notTodayButton = feedbackButton("Not Today");
    private final JButton dislikeButton = feedbackButton("Dislike");

    private final BiConsumer<String, FeedbackAction> feedbackHandler;

    private Recommendation currentRecommendation;
    private boolean detailsVisible;

    public OsrsStrategistPanel(
            BiConsumer<String, FeedbackAction> feedbackHandler)
    {
        this.feedbackHandler = feedbackHandler;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.setBorder(
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        );

        JLabel title = wrapLabel("<b>OSRS STRATEGIST</b>");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        content.add(title);
        content.add(Box.createVerticalStrut(4));
        content.add(wrapLabel("Your adaptive progression planner"));
        content.add(Box.createVerticalStrut(16));

        content.add(accountName);
        content.add(Box.createVerticalStrut(4));
        content.add(accountType);
        content.add(Box.createVerticalStrut(4));
        content.add(totalLevel);

        content.add(Box.createVerticalStrut(20));
        content.add(sectionHeader("STRATEGY"));
        content.add(strategyMode);
        content.add(sessionIntent);
        content.add(questTolerance);

        content.add(Box.createVerticalStrut(20));
        content.add(sectionHeader("DO NEXT"));
        content.add(recommendationTitle);
        content.add(Box.createVerticalStrut(5));
        content.add(recommendationBody);
        content.add(Box.createVerticalStrut(8));

        detailsButton.setAlignmentX(LEFT_ALIGNMENT);
        detailsButton.setMaximumSize(
                new Dimension(CONTENT_WIDTH, 24)
        );
        content.add(detailsButton);
        content.add(Box.createVerticalStrut(8));

        JPanel feedbackPanel = new JPanel(
                new GridLayout(2, 2, 4, 4)
        );
        feedbackPanel.setOpaque(false);
        feedbackPanel.setAlignmentX(LEFT_ALIGNMENT);
        feedbackPanel.setPreferredSize(
                new Dimension(CONTENT_WIDTH, 58)
        );
        feedbackPanel.setMaximumSize(
                new Dimension(CONTENT_WIDTH, 58)
        );
        feedbackPanel.add(doThisButton);
        feedbackPanel.add(laterButton);
        feedbackPanel.add(notTodayButton);
        feedbackPanel.add(dislikeButton);

        content.add(feedbackPanel);
        content.add(Box.createVerticalStrut(5));
        content.add(feedbackStatus);
        content.add(Box.createVerticalStrut(3));
        content.add(
                wrapLabel(
                        "<i>Feedback is remembered for this account.</i>"
                )
        );

        detailsButton.addActionListener(event -> toggleDetails());
        doThisButton.addActionListener(
                event -> submitFeedback(FeedbackAction.DO_THIS)
        );
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

        content.add(Box.createVerticalStrut(20));
        content.add(sectionHeader("OTHER GOOD OPTIONS"));
        content.add(alternativeOne);
        content.add(Box.createVerticalStrut(5));
        content.add(alternativeTwo);

        content.add(Box.createVerticalStrut(20));
        content.add(sectionHeader("OPPORTUNITIES"));
        content.add(opportunityOne);
        content.add(Box.createVerticalStrut(4));
        content.add(opportunityTwo);

        add(content, BorderLayout.NORTH);
    }

    public void updateAccount(
            String name,
            String type,
            int total)
    {
        accountName.setText(html(escape(name)));
        accountType.setText(
                html("Account type: " + escape(type))
        );
        totalLevel.setText(
                html(
                        total > 0
                                ? "Total level: " + total + " / 2376"
                                : "Total level: -- / 2376"
                )
        );
    }

    public void updateStrategy(
            StrategyMode mode,
            QuestTolerance tolerance)
    {
        updateStrategy(
                mode,
                SessionIntent.PICK_FOR_ME,
                tolerance
        );
    }

    public void updateStrategy(
            StrategyMode mode,
            SessionIntent intent,
            QuestTolerance tolerance)
    {
        strategyMode.setText(
                html("Mode: " + prettyName(mode.name()))
        );
        sessionIntent.setText(
                html("Session: " + prettyName(intent.name()))
        );
        questTolerance.setText(
                html("Quest tolerance: " + prettyName(tolerance.name()))
        );
    }

    public void updateRecommendations(
            List<Recommendation> recommendations)
    {
        if (recommendations == null || recommendations.isEmpty())
        {
            currentRecommendation = null;
            detailsVisible = false;
            detailsButton.setText("Details");
            setRecommendationButtonsEnabled(false);

            recommendationTitle.setText(
                    html("No recommendation available.")
            );
            recommendationBody.setText(html(""));
            alternativeOne.setText(html(""));
            alternativeTwo.setText(html(""));
            return;
        }

        Recommendation best = recommendations.get(0);
        String previousId = currentRecommendation == null
                ? null
                : currentRecommendation.getId();

        currentRecommendation = best;

        // A newly rotated recommendation starts collapsed. If the same task is
        // merely refreshed by a stat event, preserve the player's Details view.
        if (previousId == null || !previousId.equals(best.getId()))
        {
            detailsVisible = false;
            detailsButton.setText("Details");
        }

        setRecommendationButtonsEnabled(true);
        recommendationTitle.setText(
                html("<b>" + escape(best.getTitle()) + "</b>")
        );
        renderRecommendationBody();

        alternativeOne.setText(
                html(
                        recommendations.size() > 1
                                ? "• " + escape(
                                recommendations.get(1).getTitle()
                        )
                                : ""
                )
        );
        alternativeTwo.setText(
                html(
                        recommendations.size() > 2
                                ? "• " + escape(
                                recommendations.get(2).getTitle()
                        )
                                : ""
                )
        );
    }

    public void updateOpportunities(List<Opportunity> opportunities)
    {
        if (opportunities == null || opportunities.isEmpty())
        {
            opportunityOne.setText(
                    html("No active reminders yet.")
            );
            opportunityTwo.setText(html(""));
            return;
        }

        opportunityOne.setText(
                html(opportunityText(opportunities.get(0)))
        );
        opportunityTwo.setText(
                html(
                        opportunities.size() > 1
                                ? opportunityText(opportunities.get(1))
                                : ""
                )
        );
    }

    private void toggleDetails()
    {
        if (currentRecommendation == null)
        {
            return;
        }

        detailsVisible = !detailsVisible;
        detailsButton.setText(
                detailsVisible ? "Hide Details" : "Details"
        );
        renderRecommendationBody();
        revalidate();
        repaint();
    }

    private void renderRecommendationBody()
    {
        if (currentRecommendation == null)
        {
            recommendationBody.setText(html(""));
            return;
        }

        String body = detailsVisible
                ? RecommendationPresentation.detailedHtml(
                        currentRecommendation
                )
                : RecommendationPresentation.compactHtml(
                        currentRecommendation
                );

        recommendationBody.setText(html(body));
    }

    private void submitFeedback(FeedbackAction action)
    {
        if (currentRecommendation == null || feedbackHandler == null)
        {
            return;
        }

        String actedOnTitle = currentRecommendation.getTitle();
        String actedOnId = currentRecommendation.getId();

        feedbackStatus.setText(
                html(
                        feedbackStatusText(
                                action,
                                actedOnTitle
                        )
                )
        );

        feedbackHandler.accept(actedOnId, action);
    }

    private void setRecommendationButtonsEnabled(boolean enabled)
    {
        detailsButton.setEnabled(enabled);
        doThisButton.setEnabled(enabled);
        laterButton.setEnabled(enabled);
        notTodayButton.setEnabled(enabled);
        dislikeButton.setEnabled(enabled);
    }

    private static JButton feedbackButton(String text)
    {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setFont(button.getFont().deriveFont(11f));
        button.setMargin(new Insets(2, 2, 2, 2));
        return button;
    }

    private static String opportunityText(Opportunity opportunity)
    {
        String state = opportunity.isReady()
                ? "Ready"
                : confidenceName(opportunity.getConfidence());

        return "• <b>" + escape(opportunity.getTitle())
                + "</b> — " + state;
    }

    private static String feedbackStatusText(
            FeedbackAction action,
            String title)
    {
        String activity = escape(
                title == null ? "Recommendation" : title
        );

        switch (action)
        {
            case DO_THIS:
                return "<b>Selected:</b> " + activity;
            case LATER:
                return "<b>" + activity
                        + "</b> snoozed for 1 hour.";
            case NOT_TODAY:
                return "<b>" + activity
                        + "</b> snoozed for 24 hours.";
            case DISLIKE:
                return "<b>" + activity
                        + "</b> disliked. It will appear less often.";
            default:
                return "<b>Feedback saved.</b>";
        }
    }

    private static JLabel sectionHeader(String text)
    {
        JLabel label = wrapLabel("<b>" + text + "</b>");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private static JLabel wrapLabel(String text)
    {
        JLabel label = new JLabel(html(text));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static String prettyName(String text)
    {
        String lower = text.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0))
                + lower.substring(1);
    }

    private static String confidenceName(
            RecommendationConfidence confidence)
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
        return "<html><div style='width:"
                + CONTENT_WIDTH
                + "px;'>"
                + text
                + "</div></html>";
    }
}
