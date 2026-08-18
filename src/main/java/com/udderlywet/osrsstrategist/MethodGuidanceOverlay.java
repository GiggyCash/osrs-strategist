package com.udderlywet.osrsstrategist;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** Movable on-game checklist complementing the Compass sidebar. */
public class MethodGuidanceOverlay extends OverlayPanel
{
    private static final int OVERLAY_WIDTH = 320;
    private static final int STEP_TEXT_WIDTH = 230;

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
        if (checklist == null || checklist.getSteps().isEmpty()) return null;
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(checklist.getTitle())
                .color(StrategistTheme.GOLD)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Progress")
                .right(checklist.completeCount() + "/" + checklist.getSteps().size())
                .build());

        FontMetrics metrics = graphics.getFontMetrics();
        int shown = Math.min(12, checklist.getSteps().size());
        for (int i = 0; i < shown; i++)
        {
            GuidanceStep step = checklist.getSteps().get(i);
            List<String> lines = wrap(
                    marker(step.getState()) + " " + step.getLabel(),
                    metrics,
                    STEP_TEXT_WIDTH);
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++)
            {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left(lines.get(lineIndex))
                        .right(lineIndex == 0 ? status(step.getState()) : "")
                        .leftColor(color(step.getState()))
                        .rightColor(color(step.getState()))
                        .build());
            }
        }
        if (checklist.getSteps().size() > shown)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("+" + (checklist.getSteps().size() - shown) + " more")
                    .right("Details")
                    .leftColor(StrategistTheme.MUTED_TEXT)
                    .rightColor(StrategistTheme.MUTED_TEXT)
                    .build());
        }
        return super.render(graphics);
    }

    private static List<String> wrap(
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

    private static String marker(GuidanceStepState state)
    {
        switch (state)
        {
            case COMPLETE: return "✓";
            case WARNING:
            case BLOCKED: return "!";
            case ACTION: return "•";
            case CHECK_NEEDED:
            default: return "?";
        }
    }

    private static String status(GuidanceStepState state)
    {
        switch (state)
        {
            case COMPLETE: return "Done";
            case ACTION: return "Do";
            case WARNING: return "Fix";
            case BLOCKED: return "Blocked";
            case CHECK_NEEDED:
            default: return "Check";
        }
    }

    private static Color color(GuidanceStepState state)
    {
        switch (state)
        {
            case COMPLETE: return StrategistTheme.SUCCESS;
            case WARNING: return StrategistTheme.WARNING;
            case BLOCKED: return StrategistTheme.DANGER;
            case ACTION: return StrategistTheme.GOLD;
            case CHECK_NEEDED:
            default: return StrategistTheme.TEXT;
        }
    }
}
