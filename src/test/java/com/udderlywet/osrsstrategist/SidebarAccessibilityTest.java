package com.udderlywet.osrsstrategist;

import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SidebarAccessibilityTest
{
    @Test
    public void repeatedScalingIsIdempotentAndIncludesWrappedText()
    {
        JPanel root = new JPanel();
        JTextArea text = new JTextArea("Wrapped recommendation");
        JButton button = new JButton("Later");
        button.setPreferredSize(new Dimension(80, 28));
        root.add(text);
        root.add(button);
        float original = text.getFont().getSize2D();

        SidebarAccessibility.apply(root, SidebarTextSize.LARGE);
        float scaled = text.getFont().getSize2D();
        int height = button.getPreferredSize().height;
        SidebarAccessibility.apply(root, SidebarTextSize.LARGE);

        assertTrue(scaled > original);
        assertEquals(scaled, text.getFont().getSize2D(), 0.01f);
        assertEquals(height, button.getPreferredSize().height);
    }

    @Test
    public void returningToStandardRestoresOriginalMetrics()
    {
        JPanel root = new JPanel();
        JTextArea text = new JTextArea("Recommendation");
        JButton button = new JButton("Details");
        button.setPreferredSize(new Dimension(90, 30));
        root.add(text);
        root.add(button);
        float font = text.getFont().getSize2D();
        int height = button.getPreferredSize().height;

        SidebarAccessibility.apply(root, SidebarTextSize.EXTRA_LARGE);
        SidebarAccessibility.apply(root, SidebarTextSize.STANDARD);

        assertEquals(font, text.getFont().getSize2D(), 0.01f);
        assertEquals(height, button.getPreferredSize().height);
    }

    @Test
    public void largerTextReflowsFixedWidthRecommendationCopy()
    {
        JPanel root = new JPanel();
        JTextArea text = new JTextArea(
                "Withdraw iron bars, smith iron two-handed swords, bank, and repeat.");
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        Dimension fixed = new Dimension(130, 30);
        text.setPreferredSize(fixed);
        text.setMinimumSize(fixed);
        text.setMaximumSize(fixed);
        root.add(text);

        SidebarAccessibility.apply(root, SidebarTextSize.STANDARD);
        int standardHeight = text.getPreferredSize().height;
        SidebarAccessibility.apply(root, SidebarTextSize.EXTRA_LARGE);

        assertTrue(text.getPreferredSize().height > standardHeight);
        assertEquals(130, text.getPreferredSize().width);
    }
}
