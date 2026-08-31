package compass;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.*;
import javax.swing.text.JTextComponent;

/** Idempotent font and control scaling scoped to the Compass component tree. */
final class SidebarAccessibility
{
    private static final String BASE_FONT = Text.get(1943);
    private static final String BASE_PREFERRED = Text.get(1944);
    private static final String BASE_MINIMUM = Text.get(1945);
    private static final String BASE_MAXIMUM = Text.get(1946);

    private SidebarAccessibility() {}

    static void apply(Container root, SidebarTextSize size)
    {
        if (root == null) return;
        var scale = size == null ? 1.0f : size.getScale();
        scaleChildren(root, scale);
        root.revalidate();
        root.repaint();
    }

    private static void scaleChildren(Container parent, float scale)
    {
        for (Component component : parent.getComponents())
        {
            if (component instanceof JLabel
                    || component instanceof JTextComponent
                    || component instanceof AbstractButton)
                scaleFont((JComponent) component, scale);
            if (component instanceof JTextArea)
                reflowFixedWidthText((JTextArea) component);
            if (component instanceof AbstractButton)
                scaleHeight((JComponent) component, scale);
            if (component instanceof Container)
                scaleChildren((Container) component, scale);
        }
    }

    private static void scaleFont(JComponent component, float scale)
    {
        var current = component.getFont();
        if (current == null) return;
        var base = (Font) component.getClientProperty(BASE_FONT);
        if (base == null)
        {
            base = current;
            component.putClientProperty(BASE_FONT, base);
        }
        component.setFont(base.deriveFont(base.getSize2D() * scale));
    }

    private static void scaleHeight(JComponent component, float scale)
    {
        Dimension preferred = base(component, BASE_PREFERRED,
                component.getPreferredSize());
        Dimension minimum = base(component, BASE_MINIMUM,
                component.getMinimumSize());
        Dimension maximum = base(component, BASE_MAXIMUM,
                component.getMaximumSize());
        var bonus = Math.round(Math.max(0f, scale - 1f) * 22f);
        component.setPreferredSize(withHeight(preferred, bonus));
        component.setMinimumSize(withHeight(minimum, bonus));
        if (maximum != null && maximum.height < Integer.MAX_VALUE)
            component.setMaximumSize(withHeight(maximum, bonus));
    }

    /** Recompute BoxLayout's fixed text height after a font-size change. */
    private static void reflowFixedWidthText(JTextArea area)
    {
        var maximum = area.getMaximumSize();
        if (maximum == null || maximum.width <= 0
                || maximum.height == Integer.MAX_VALUE) return;
        var width = maximum.width;
        area.setPreferredSize(null);
        area.setMinimumSize(null);
        area.setMaximumSize(null);
        area.setSize(new Dimension(width, 10_000));
        var measured = area.getPreferredSize();
        var lineHeight = area.getFontMetrics(area.getFont()).getHeight();
        int height = area.getText() == null || area.getText().isEmpty()
                ? 1 : Math.max(lineHeight, measured.height);
        var fixed = new Dimension(width, height);
        area.setPreferredSize(fixed);
        area.setMinimumSize(fixed);
        area.setMaximumSize(fixed);
    }

    private static Dimension base(JComponent component, String key,
            Dimension current)
    {
        var stored = (Dimension) component.getClientProperty(key);
        if (stored != null) return stored;
        if (current == null) return null;
        stored = new Dimension(current);
        component.putClientProperty(key, stored);
        return stored;
    }

    private static Dimension withHeight(Dimension value, int bonus)
    {
        return value == null ? null
                : new Dimension(value.width, value.height + bonus);
    }
}
