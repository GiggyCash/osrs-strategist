package com.udderlywet.osrsstrategist;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProgressViewPanelTest
{
    @Test
    public void rendersRealSessionAtNarrowWidthAndLargeText()
    {
        ProgressAnalyticsService service = new ProgressAnalyticsService();
        service.reset(0L);
        service.record(Skill.AGILITY, 1_000, 10, 0L);
        service.record(Skill.AGILITY, 2_500, 12, 60_000L);
        service.setTarget(new ProgressTarget("skill:agility", "rooftop",
                Skill.AGILITY, 20));

        ProgressViewPanel panel = new ProgressViewPanel(1.6f);
        panel.setSnapshot(service.snapshot(60_000L));
        paint(panel, 140, 520);

        assertEquals("1,500 XP", panel.getSessionXpText());
        assertTrue(panel.getTargetText().contains("Agility to 20"));
        assertTrue(panel.getTargetText().contains("calculating ETA"));
        assertEquals(1, panel.getChart().getRenderedBucketCount());
    }

    @Test
    public void emptyStateRendersWithoutFakeRateOrEta()
    {
        ProgressViewPanel panel = new ProgressViewPanel();
        panel.setSnapshot(null);
        paint(panel, 220, 420);

        assertEquals("0 XP", panel.getSessionXpText());
        assertEquals("No progress this session", panel.getSessionMetaText());
        assertEquals("No active skill target", panel.getTargetText());
    }

    @Test
    public void lastSessionRecapUsesBoundedCharacterHistory()
    {
        ProgressHistory history = new ProgressHistory();
        history.replaceAll(Collections.singletonList(
                new ProgressSessionSummary(0L, 3_600_000L, 2_400_000L,
                        87_420L, 3,
                        Collections.singletonMap(Skill.FISHING, 87_420))),
                Collections.emptyList(), Collections.emptyList());
        ProgressViewPanel panel = new ProgressViewPanel();
        panel.setHistory(history);
        paint(panel, 220, 520);

        assertTrue(panel.getLastSessionText().contains("+87,420 XP"));
        assertTrue(panel.getLastSessionText().contains("3 levels"));
        assertTrue(panel.getLastSessionText().contains("Fishing +87,420"));
    }

    private static void paint(ProgressViewPanel panel, int width, int height)
    {
        panel.setSize(width, height);
        panel.doLayout();
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try
        {
            panel.paint(graphics);
        }
        finally
        {
            graphics.dispose();
        }
    }
}
