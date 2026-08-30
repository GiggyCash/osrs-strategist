package com.udderlywet.osrsstrategist;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** Short-lived CLOG-style reward surface for any Strategist completion. */
public class MilestoneRewardOverlay extends OverlayPanel
{
    private static final long DISPLAY_MILLIS = 6500L;

    private StrategistRewardNotification activeReward;
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
        show(StrategistRewardNotification.fromMilestone(completion));
    }

    public synchronized void show(StrategistRewardNotification reward)
    {
        activeReward = reward;
        hideAtMillis = reward == null
                ? 0L
                : System.currentTimeMillis() + DISPLAY_MILLIS;
    }

    public synchronized void clear()
    {
        activeReward = null;
        hideAtMillis = 0L;
    }

    @Override
    public synchronized Dimension render(Graphics2D graphics)
    {
        if (activeReward == null
                || System.currentTimeMillis() >= hideAtMillis)
        {
            clear();
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text(activeReward.getHeader())
                        .color(StrategistTheme.GOLD)
                        .build()
        );
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left(activeReward.getLeft())
                        .right(activeReward.getRight())
                        .build()
        );
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left(activeReward.getFooterLeft())
                        .right(activeReward.getFooterRight())
                        .build()
        );

        return super.render(graphics);
    }
}
