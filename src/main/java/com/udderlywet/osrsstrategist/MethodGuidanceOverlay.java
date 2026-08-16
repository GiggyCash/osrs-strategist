package com.udderlywet.osrsstrategist;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Movable on-game checklist complementing the Strategist sidebar.
 *
 * <p>The checklist is intentionally larger than RuneLite's default overlay
 * component width. Guidance is something the player reads while also watching
 * the game, so a few extra pixels and a slightly larger font are preferable to
 * squeezing long requirement names into tiny fragments.</p>
 */
public class MethodGuidanceOverlay extends OverlayPanel
{
    private static final int OVERLAY_WIDTH = 285;
    private static final float OVERLAY_FONT_SIZE = 12.5f;

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

        Font previousFont = graphics.getFont();
        graphics.setFont(previousFont.deriveFont(OVERLAY_FONT_SIZE));
        try
        {
            panelComponent.setPreferredSize(new Dimension(OVERLAY_WIDTH, 0));
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
        finally
        {
            graphics.setFont(previousFont);
        }
    }

    /**
     * Hollow circle means "check this". It is deliberately not a question mark:
     * unknown evidence is a normal state in an account-aware planner, not an
     * error and not unfinished placeholder UI.
     */
    private static String marker(GuidanceStepState state)
    {
        switch (state)
        {
            case COMPLETE: return "✓";
            case WARNING:
            case BLOCKED: return "!";
            case ACTION: return "•";
            case CHECK_NEEDED:
            default: return "○";
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
