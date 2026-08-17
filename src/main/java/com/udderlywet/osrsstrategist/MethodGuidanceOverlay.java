package com.udderlywet.osrsstrategist;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Movable in-game checklist complementing the Strategist sidebar.
 *
 * <p>The overlay is deliberately wider than RuneLite's default compact panels.
 * Requirement labels frequently contain item names, quest/access requirements,
 * or transport notes, so a little extra width significantly improves legibility
 * without covering a large portion of the game view.</p>
 */
public class MethodGuidanceOverlay extends OverlayPanel
{
    private GuidanceChecklist checklist;

    @Inject
    public MethodGuidanceOverlay(OsrsStrategistPlugin plugin)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setMovable(true);
        panelComponent.setPreferredSize(new Dimension(285, 0));
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

        int shown = Math.min(12, checklist.getSteps().size());
        for (int i = 0; i < shown; i++)
        {
            GuidanceStep step = checklist.getSteps().get(i);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(marker(step.getState()) + " " + step.getLabel())
                    .right(status(step.getState()))
                    .leftColor(color(step.getState()))
                    .rightColor(color(step.getState()))
                    .build());
        }
        if (checklist.getSteps().size() > shown)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("+" + (checklist.getSteps().size() - shown) + " more")
                    .right("Sidebar")
                    .leftColor(StrategistTheme.MUTED_TEXT)
                    .rightColor(StrategistTheme.MUTED_TEXT)
                    .build());
        }
        return super.render(graphics);
    }

    /**
     * A question mark looked like a missing asset/placeholder in live testing.
     * Unknown state now uses the same neutral bullet language as the sidebar;
     * the explicit right-side "Check" label carries the meaning.
     */
    private static String marker(GuidanceStepState state)
    {
        switch (state)
        {
            case COMPLETE: return "✓";
            case WARNING:
            case BLOCKED: return "!";
            case ACTION:
            case CHECK_NEEDED:
            default: return "•";
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
