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

public class OsrsStrategistPanel extends PluginPanel
{
    private static final int CONTENT_WIDTH = 160;

    private final JLabel accountName =
            wrapLabel("Waiting for login...");

    private final JLabel accountType =
            wrapLabel("Account type: Unknown");

    private final JLabel totalLevel =
            wrapLabel("Total level: -- / 2376");

    private final JLabel strategyMode =
            wrapLabel("Mode: Balanced");

    private final JLabel questTolerance =
            wrapLabel("Quest tolerance: Normal");

    private final JLabel recommendationTitle =
            wrapLabel("Analyzing account...");

    private final JLabel recommendationReason =
            wrapLabel("");

    private final JLabel feedbackStatus =
            wrapLabel("");

    private final JLabel alternativeOne =
            wrapLabel("");

    private final JLabel alternativeTwo =
            wrapLabel("");

    private final JButton doThisButton =
            feedbackButton("Do This");

    private final JButton laterButton =
            feedbackButton("Later");

    private final JButton notTodayButton =
            feedbackButton("Not Today");

    private final JButton dislikeButton =
            feedbackButton("Dislike");

    private final BiConsumer<String, FeedbackAction> feedbackHandler;

    private String currentRecommendationId;

    public OsrsStrategistPanel(
            BiConsumer<String, FeedbackAction> feedbackHandler)
    {
        this.feedbackHandler = feedbackHandler;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel content = new JPanel();

        content.setLayout(
                new BoxLayout(
                        content,
                        BoxLayout.Y_AXIS
                )
        );

        content.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        content.setBorder(
                BorderFactory.createEmptyBorder(
                        8,
                        8,
                        8,
                        8
                )
        );

        JLabel title =
                wrapLabel(
                        "<b>OSRS STRATEGIST</b>"
                );

        title.setFont(
                title.getFont()
                        .deriveFont(
                                Font.BOLD,
                                16f
                        )
        );

        JLabel subtitle =
                wrapLabel(
                        "Your adaptive progression planner"
                );

        content.add(title);
        content.add(Box.createVerticalStrut(4));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(16));

        content.add(accountName);
        content.add(Box.createVerticalStrut(4));
        content.add(accountType);
        content.add(Box.createVerticalStrut(4));
        content.add(totalLevel);

        content.add(Box.createVerticalStrut(20));
        content.add(sectionHeader("STRATEGY"));
        content.add(strategyMode);
        content.add(questTolerance);

        content.add(Box.createVerticalStrut(20));
        content.add(sectionHeader("DO NEXT"));
        content.add(recommendationTitle);
        content.add(Box.createVerticalStrut(5));
        content.add(recommendationReason);
        content.add(Box.createVerticalStrut(10));

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

        setFeedbackButtonsEnabled(false);

        content.add(Box.createVerticalStrut(20));
        content.add(sectionHeader("OTHER GOOD OPTIONS"));
        content.add(alternativeOne);
        content.add(Box.createVerticalStrut(5));
        content.add(alternativeTwo);

        content.add(Box.createVerticalStrut(20));
        content.add(sectionHeader("OPPORTUNITIES"));
        content.add(
                wrapLabel(
                        "No active reminders yet."
                )
        );

        add(
                content,
                BorderLayout.NORTH
        );
    }

    public void updateAccount(
            String name,
            String type,
            int total)
    {
        accountName.setText(
                html(name)
        );

        accountType.setText(
                html(
                        "Account type: "
                                + type
                )
        );

        if (total > 0)
        {
            totalLevel.setText(
                    html(
                            "Total level: "
                                    + total
                                    + " / 2376"
                    )
            );
        }
        else
        {
            totalLevel.setText(
                    html(
                            "Total level: -- / 2376"
                    )
            );
        }
    }

    public void updateStrategy(
            StrategyMode mode,
            QuestTolerance tolerance)
    {
        strategyMode.setText(
                html(
                        "Mode: "
                                + prettyName(
                                mode.name()
                        )
                )
        );

        questTolerance.setText(
                html(
                        "Quest tolerance: "
                                + prettyName(
                                tolerance.name()
                        )
                )
        );
    }

    public void updateRecommendations(
            List<Recommendation> recommendations)
    {
        if (recommendations == null
                || recommendations.isEmpty())
        {
            currentRecommendationId = null;
            setFeedbackButtonsEnabled(false);

            recommendationTitle.setText(
                    html(
                            "No recommendation available."
                    )
            );

            recommendationReason.setText(
                    html("")
            );

            alternativeOne.setText(
                    html("")
            );

            alternativeTwo.setText(
                    html("")
            );

            return;
        }

        Recommendation best =
                recommendations.get(0);

        currentRecommendationId = best.getId();
        setFeedbackButtonsEnabled(true);

        recommendationTitle.setText(
                html(
                        "<b>"
                                + best.getTitle()
                                + "</b>"
                )
        );

        recommendationReason.setText(
                html(
                        best.getReason()
                )
        );

        if (recommendations.size() > 1)
        {
            alternativeOne.setText(
                    html(
                            "• "
                                    + recommendations
                                    .get(1)
                                    .getTitle()
                    )
            );
        }
        else
        {
            alternativeOne.setText(
                    html("")
            );
        }

        if (recommendations.size() > 2)
        {
            alternativeTwo.setText(
                    html(
                            "• "
                                    + recommendations
                                    .get(2)
                                    .getTitle()
                    )
            );
        }
        else
        {
            alternativeTwo.setText(
                    html("")
            );
        }
    }

    private void submitFeedback(
            FeedbackAction action)
    {
        if (currentRecommendationId == null
                || feedbackHandler == null)
        {
            return;
        }

        feedbackStatus.setText(
                html(
                        feedbackStatusText(action)
                )
        );

        feedbackHandler.accept(
                currentRecommendationId,
                action
        );
    }

    private void setFeedbackButtonsEnabled(
            boolean enabled)
    {
        doThisButton.setEnabled(enabled);
        laterButton.setEnabled(enabled);
        notTodayButton.setEnabled(enabled);
        dislikeButton.setEnabled(enabled);
    }

    private static JButton feedbackButton(
            String text)
    {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setFont(
                button.getFont()
                        .deriveFont(11f)
        );
        button.setMargin(
                new Insets(2, 2, 2, 2)
        );
        return button;
    }

    private static String feedbackStatusText(
            FeedbackAction action)
    {
        switch (action)
        {
            case DO_THIS:
                return "<b>Saved:</b> Do This";

            case LATER:
                return "<b>Snoozed:</b> 1 hour";

            case NOT_TODAY:
                return "<b>Snoozed:</b> 24 hours";

            case DISLIKE:
                return "<b>Saved:</b> Dislike. Showing less often.";

            default:
                return "<b>Saved.</b>";
        }
    }

    private static JLabel sectionHeader(
            String text)
    {
        JLabel label =
                wrapLabel(
                        "<b>"
                                + text
                                + "</b>"
                );

        label.setFont(
                label.getFont()
                        .deriveFont(
                                Font.BOLD
                        )
        );

        return label;
    }

    private static JLabel wrapLabel(
            String text)
    {
        JLabel label =
                new JLabel(
                        html(text)
                );

        label.setAlignmentX(
                LEFT_ALIGNMENT
        );

        return label;
    }

    private static String prettyName(
            String text)
    {
        String lower =
                text.toLowerCase()
                        .replace('_', ' ');

        return Character.toUpperCase(
                lower.charAt(0)
        ) + lower.substring(1);
    }

    private static String html(
            String text)
    {
        return "<html><div style='width:"
                + CONTENT_WIDTH
                + "px;'>"
                + text
                + "</div></html>";
    }
}
