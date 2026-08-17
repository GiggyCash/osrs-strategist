package com.udderlywet.osrsstrategist;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Accessibility scaling for the Strategist component tree only.
 *
 * <p>RuneLite sidebars have constrained horizontal space, so widening the panel
 * itself is not dependable across layouts. Strategist therefore improves
 * readability by scaling text and fixed-height controls/rows while preserving
 * the existing wrapping behavior.</p>
 *
 * <p>The implementation is intentionally idempotent. Base fonts and dimensions
 * are stored as component client properties the first time the scaler sees a
 * component. Later config changes always derive from those original values
 * instead of multiplying an already-scaled font, button, or container height.</p>
 */
public final class SidebarAccessibility
{
    private static final String BASE_FONT_KEY =
            "osrs-strategist.accessibility.base-font";
    private static final String BASE_PREFERRED_KEY =
            "osrs-strategist.accessibility.base-preferred";
    private static final String BASE_MAXIMUM_KEY =
            "osrs-strategist.accessibility.base-maximum";
    private static final String BASE_MINIMUM_KEY =
            "osrs-strategist.accessibility.base-minimum";

    private SidebarAccessibility() {}

    public static void improveReadability(Container root)
    {
        improveReadability(root, SidebarTextSize.LARGE);
    }

    public static void improveReadability(
            Container root,
            SidebarTextSize textSize)
    {
        if (root == null) return;
        SidebarTextSize safeSize = textSize == null
                ? SidebarTextSize.LARGE
                : textSize;
        scaleRecursively(root, safeSize.getScale());
        root.revalidate();
        root.repaint();
    }

    private static void scaleRecursively(
            Container container,
            float scale)
    {
        for (Component component : container.getComponents())
        {
            if (component instanceof JLabel)
            {
                scaleFont((JComponent) component, scale);
            }
            else if (component instanceof AbstractButton)
            {
                scaleFont((JComponent) component, scale);
                scaleFixedHeight((JComponent) component, scale);
            }

            if (component instanceof Container)
            {
                // Some sidebar rows intentionally cap their height (for example
                // the recommendation title row and the feedback-button strip).
                // If their children grow but the row does not, Swing can clip
                // the lower edge of text even though the child itself reports a
                // larger preferred size. Expand only genuinely fixed-height
                // containers; normal vertically-growing cards are untouched.
                if (component instanceof JComponent)
                {
                    scaleFixedContainerHeight((JComponent) component, scale);
                }
                scaleRecursively((Container) component, scale);
            }
        }
    }

    private static void scaleFont(JComponent component, float scale)
    {
        Font current = component.getFont();
        if (current == null) return;

        Font base = (Font) component.getClientProperty(BASE_FONT_KEY);
        if (base == null)
        {
            base = current;
            component.putClientProperty(BASE_FONT_KEY, base);
        }

        component.setFont(base.deriveFont(base.getSize2D() * scale));
    }

    private static void scaleFixedContainerHeight(
            JComponent component,
            float scale)
    {
        Dimension preferred = component.getPreferredSize();
        Dimension maximum = component.getMaximumSize();

        if (preferred == null || maximum == null) return;
        if (maximum.height == Integer.MAX_VALUE) return;

        // A finite maximum height is a strong signal that this is a deliberate
        // fixed-height row. Increase it in lockstep with its children.
        scaleFixedHeight(component, scale);
    }

    private static void scaleFixedHeight(
            JComponent component,
            float scale)
    {
        Dimension preferred = baseDimension(
                component, BASE_PREFERRED_KEY, component.getPreferredSize());
        Dimension maximum = baseDimension(
                component, BASE_MAXIMUM_KEY, component.getMaximumSize());
        Dimension minimum = baseDimension(
                component, BASE_MINIMUM_KEY, component.getMinimumSize());

        int bonus = Math.round(Math.max(0f, scale - 1f) * 20f);

        if (preferred != null)
        {
            component.setPreferredSize(new Dimension(
                    preferred.width,
                    preferred.height + bonus));
        }

        if (maximum != null && maximum.height < Integer.MAX_VALUE)
        {
            component.setMaximumSize(new Dimension(
                    maximum.width,
                    maximum.height + bonus));
        }

        if (minimum != null)
        {
            component.setMinimumSize(new Dimension(
                    minimum.width,
                    minimum.height + bonus));
        }
    }

    private static Dimension baseDimension(
            JComponent component,
            String key,
            Dimension current)
    {
        Dimension stored = (Dimension) component.getClientProperty(key);
        if (stored != null) return stored;
        if (current == null) return null;

        Dimension copy = new Dimension(current);
        component.putClientProperty(key, copy);
        return copy;
    }
}
