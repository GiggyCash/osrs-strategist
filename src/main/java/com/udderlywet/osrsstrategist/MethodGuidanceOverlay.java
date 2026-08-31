package com.udderlywet.osrsstrategist;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.*;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** Movable on-game checklist complementing the Compass sidebar. */
public class MethodGuidanceOverlay extends OverlayPanel
{
    private static final int OVERLAY_WIDTH = 320;
    private static final int STEP_TEXT_WIDTH = 285;

    private GuidanceChecklist checklist;

    @Inject
    public MethodGuidanceOverlay(OsrsStrategistPlugin plugin)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setMovable(true);
        panelComponent.setPreferredSize(new Dimension(OVERLAY_WIDTH, 0));
    }

    public synchronized void update(GuidanceChecklist checklist)
    {
        this.checklist = checklist;
    }

    public synchronized void clear()
    {
        checklist = null;
    }

    @Override
    public synchronized Dimension render(Graphics2D graphics)
    {
        if (checklist == null) return null;
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Method Guidance")
                .color(StrategistTheme.GOLD)
                .build());
        FontMetrics metrics = graphics.getFontMetrics();
        addSection("METHOD", checklist.getTitle(), metrics);
        addSection("BRING", checklist.getBring(), metrics);
        addSection("WHERE", checklist.getWhere(), metrics);
        addSection("DO", fallbackAction(checklist), metrics);
        addSection("PROGRESS", checklist.getProgress(), metrics);
        addSection("IMPORTANT", checklist.getImportant(), metrics);
        return super.render(graphics);
    }

    private void addSection(String heading, String value, FontMetrics metrics)
    {
        if (value == null || value.trim().isEmpty()) return;
        panelComponent.getChildren().add(LineComponent.builder()
                .left(heading)
                .leftColor("IMPORTANT".equals(heading)
                        ? StrategistTheme.WARNING : StrategistTheme.GOLD_SOFT)
                .build());
        for (String line : wrap(value, metrics, STEP_TEXT_WIDTH))
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(line)
                    .leftColor("IMPORTANT".equals(heading)
                            ? StrategistTheme.WARNING : StrategistTheme.TEXT)
                    .build());
    }

    private static String fallbackAction(GuidanceChecklist checklist)
    {
        if (checklist.getAction() != null
                && !checklist.getAction().trim().isEmpty())
            return checklist.getAction();
        GuidanceStep pending = checklist.firstPending();
        return pending == null ? checklist.getSubtitle() : pending.getLabel();
    }

    static List<String> wrap(
            String text,
            FontMetrics metrics,
            int maxWidth)
    {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty())
        {
            lines.add("");
            return lines;
        }

        StringBuilder current = new StringBuilder();
        for (String word : text.trim().split("\\s+"))
        {
            String candidate = current.length() == 0
                    ? word : current + " " + word;
            if (current.length() > 0
                    && metrics.stringWidth(candidate) > maxWidth)
            {
                lines.add(current.toString());
                current.setLength(0);
            }

            if (metrics.stringWidth(word) <= maxWidth)
            {
                if (current.length() > 0) current.append(' ');
                current.append(word);
                continue;
            }

            // Very long unbroken labels are split by measured character width
            // rather than allowed to bleed out of the overlay.
            StringBuilder fragment = new StringBuilder();
            for (int i = 0; i < word.length(); i++)
            {
                char ch = word.charAt(i);
                if (fragment.length() > 0
                        && metrics.stringWidth(fragment.toString() + ch) > maxWidth)
                {
                    lines.add(fragment.toString());
                    fragment.setLength(0);
                }
                fragment.append(ch);
            }
            if (fragment.length() > 0)
            {
                if (current.length() > 0) current.append(' ');
                current.append(fragment);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        if (lines.isEmpty()) lines.add(text);
        return lines;
    }

}
