package com.udderlywet.osrsstrategist;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Small accessibility pass for the RuneLite sidebar.
 *
 * <p>RuneLite side panels have limited horizontal space, so simply forcing a
 * wider panel is unreliable across client layouts. Instead Strategist increases
 * text size modestly and allows labels to wrap vertically. Fixed-height controls
 * receive a matching height increase so larger text never gets cropped.</p>
 *
 * <p>This deliberately avoids changing RuneLite's global UI defaults. Only the
 * Strategist component tree is touched, which means users keep their normal
 * client appearance everywhere else.</p>
 */
public final class SidebarAccessibility
{
    private static final float LABEL_SCALE = 1.12f;
    private static final float BUTTON_SCALE = 1.10f;
    private static final int CONTROL_HEIGHT_BONUS = 4;

    private SidebarAccessibility() {}

    public static void improveReadability(Container root)
    {
        if (root == null) return;
        scaleRecursively(root);
        root.revalidate();
        root.repaint();
    }

    private static void scaleRecursively(Container container)
    {
        for (Component component : container.getComponents())
        {
            if (component instanceof JLabel)
            {
                scaleFont(component, LABEL_SCALE);
            }
            else if (component instanceof AbstractButton)
            {
                scaleFont(component, BUTTON_SCALE);
                increaseControlHeight((JComponent) component);
            }

            if (component instanceof Container)
            {
                scaleRecursively((Container) component);
            }
        }
    }

    private static void scaleFont(Component component, float scale)
    {
        Font font = component.getFont();
        if (font == null) return;
        component.setFont(font.deriveFont(font.getSize2D() * scale));
    }

    private static void increaseControlHeight(JComponent component)
    {
        Dimension preferred = component.getPreferredSize();
        if (preferred != null)
        {
            component.setPreferredSize(new Dimension(
                    preferred.width,
                    preferred.height + CONTROL_HEIGHT_BONUS));
        }

        Dimension maximum = component.getMaximumSize();
        if (maximum != null && maximum.height < Integer.MAX_VALUE)
        {
            component.setMaximumSize(new Dimension(
                    maximum.width,
                    maximum.height + CONTROL_HEIGHT_BONUS));
        }

        Dimension minimum = component.getMinimumSize();
        if (minimum != null)
        {
            component.setMinimumSize(new Dimension(
                    minimum.width,
                    minimum.height + CONTROL_HEIGHT_BONUS));
        }
    }
}
