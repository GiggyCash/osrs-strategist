package com.udderlywet.osrsstrategist;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class OsrsStrategistPanel extends PluginPanel
{
    private static final int CONTENT_WIDTH = 210;

    private final JLabel accountName = wrapLabel("Waiting for login...");
    private final JLabel accountType = wrapLabel("Account type: Unknown");
    private final JLabel totalLevel = wrapLabel("Total level: -- / 2376");

    public OsrsStrategistPanel()
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = wrapLabel("<b>OSRS STRATEGIST</b>");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel subtitle = wrapLabel(
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
        content.add(wrapLabel("Mode: Balanced"));
        content.add(wrapLabel("Quest tolerance: Normal"));

        content.add(Box.createVerticalStrut(20));

        content.add(sectionHeader("DO NEXT"));
        content.add(wrapLabel("Analyzing account..."));

        content.add(Box.createVerticalStrut(20));

        content.add(sectionHeader("OPPORTUNITIES"));
        content.add(wrapLabel("No active reminders yet."));

        add(content, BorderLayout.NORTH);
    }

    public void updateAccount(String name, String type, int total)
    {
        accountName.setText(html(name));
        accountType.setText(html("Account type: " + type));
        totalLevel.setText(html("Total level: " + total + " / 2376"));
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

    private static String html(String text)
    {
        return "<html><div style='width:"
                + CONTENT_WIDTH
                + "px;'>"
                + text
                + "</div></html>";
    }
}