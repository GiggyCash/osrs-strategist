package com.udderlywet.osrsstrategist;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Short-lived top-center reward card for completed Strategist checkpoints.
 *
 * <p>It intentionally behaves more like a Collection Log notification than a
 * permanent overlay: celebrate the win, then disappear and return attention to
 * gameplay. Future non-skill short-term goals can reuse the same reward surface.</p>
 */
public class MilestoneRewardOverlay extends OverlayPanel
{
    private static final long DISPLAY_MILLIS = 6500L;

    private MilestoneCompletion activeCompletion;
    private long hideAtMillis;

    @Inject
    public MilestoneRewardOverlay(OsrsStrategistPlugin plugin)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_CENTER);
        setMovable(false);
    }

    public synchronized void show(MilestoneCompletion completion)
    {
        activeCompletion = completion;
        hideAtMillis = System.currentTimeMillis() + DISPLAY_MILLIS;
    }

    public synchronized void clear()
    {
        activeCompletion = null;
        hideAtMillis = 0L;
    }

    @Override
    public synchronized Dimension render(Graphics2D graphics)
    {
        if (activeCompletion == null
                || System.currentTimeMillis() >= hideAtMillis)
        {
            clear();
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text("STRATEGIST MILESTONE")
                        .color(StrategistTheme.GOLD)
                        .build()
        );
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left(activeCompletion.getSkill().getName())
                        .right(activeCompletion.getStartedAtLevel()
                                + " → "
                                + activeCompletion.getTargetLevel())
                        .build()
        );
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("Goal complete")
                        .right("Next move ready")
                        .build()
        );

        return super.render(graphics);
    }
}
