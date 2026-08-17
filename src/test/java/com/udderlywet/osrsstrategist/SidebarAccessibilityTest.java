package com.udderlywet.osrsstrategist;

import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SidebarAccessibilityTest
{
    @Test
    public void repeatedLargeScalingDoesNotCompound()
    {
        JPanel root = new JPanel();
        JLabel label = new JLabel("Readable text");
        JButton button = new JButton("Later");
        button.setPreferredSize(new Dimension(80, 24));
        root.add(label);
        root.add(button);

        Font original = label.getFont();
        SidebarAccessibility.improveReadability(root, SidebarTextSize.LARGE);
        float firstSize = label.getFont().getSize2D();
        int firstHeight = button.getPreferredSize().height;

        SidebarAccessibility.improveReadability(root, SidebarTextSize.LARGE);

        assertEquals(firstSize, label.getFont().getSize2D(), 0.01f);
        assertEquals(firstHeight, button.getPreferredSize().height);
        assertTrue(firstSize > original.getSize2D());
    }

    @Test
    public void switchingBackToStandardUsesOriginalMetrics()
    {
        JPanel root = new JPanel();
        JLabel label = new JLabel("Readable text");
        JButton button = new JButton("Details");
        button.setPreferredSize(new Dimension(90, 26));
        root.add(label);
        root.add(button);

        float originalFontSize = label.getFont().getSize2D();
        int originalHeight = button.getPreferredSize().height;

        SidebarAccessibility.improveReadability(root, SidebarTextSize.EXTRA_LARGE);
        SidebarAccessibility.improveReadability(root, SidebarTextSize.STANDARD);

        assertEquals(originalFontSize, label.getFont().getSize2D(), 0.01f);
        assertEquals(originalHeight, button.getPreferredSize().height);
    }

    @Test
    public void fixedHeightParentRowsGrowWithLargerText()
    {
        JPanel root = new JPanel();
        JPanel fixedRow = new JPanel();
        fixedRow.setPreferredSize(new Dimension(180, 27));
        fixedRow.setMaximumSize(new Dimension(180, 27));
        fixedRow.add(new JButton("Not Today"));
        root.add(fixedRow);

        SidebarAccessibility.improveReadability(
                root,
                SidebarTextSize.EXTRA_LARGE);

        assertTrue(fixedRow.getPreferredSize().height > 27);
        assertTrue(fixedRow.getMaximumSize().height > 27);
    }
}
