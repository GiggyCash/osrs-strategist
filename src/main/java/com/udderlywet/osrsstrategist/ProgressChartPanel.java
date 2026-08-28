package com.udderlywet.osrsstrategist;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;

/** Lightweight, dependency-free XP-over-time chart for RuneLite's sidebar. */
public final class ProgressChartPanel extends JPanel
{
    private static final int DEFAULT_HEIGHT = 112;
    private List<ProgressTimeBucket> buckets = Collections.emptyList();

    public ProgressChartPanel()
    {
        setOpaque(true);
        setBackground(StrategistTheme.CARD);
        setForeground(StrategistTheme.GOLD);
        setPreferredSize(new Dimension(180, DEFAULT_HEIGHT));
        setMinimumSize(new Dimension(80, DEFAULT_HEIGHT));
    }

    public void setBuckets(List<ProgressTimeBucket> values)
    {
        buckets = values == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
        repaint();
    }

    public int getRenderedBucketCount()
    {
        return buckets.size();
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            int left = 7;
            int right = Math.max(left + 1, width - 7);
            int top = 8;
            int bottom = Math.max(top + 1, height - 18);

            if (buckets.isEmpty() || totalXp(buckets) <= 0L)
            {
                String message = "Waiting for XP drops";
                FontMetrics metrics = g.getFontMetrics();
                int x = Math.max(5, (width - metrics.stringWidth(message)) / 2);
                g.setColor(StrategistTheme.MUTED_TEXT);
                g.drawString(message, x, Math.max(metrics.getAscent() + 3,
                        height / 2));
                return;
            }

            g.setColor(StrategistTheme.DIVIDER);
            g.setStroke(new BasicStroke(1f));
            g.drawLine(left, bottom, right, bottom);

            int available = Math.max(1, right - left);
            int displayed = Math.min(buckets.size(), available);
            int start = buckets.size() - displayed;
            int max = 1;
            for (int i = start; i < buckets.size(); i++)
                max = Math.max(max, buckets.get(i).getTotalXp());

            double step = available / (double) displayed;
            int barWidth = Math.max(1, (int) Math.floor(step * 0.72));
            int chartHeight = Math.max(1, bottom - top);
            g.setColor(StrategistTheme.GOLD);
            for (int index = 0; index < displayed; index++)
            {
                int xp = buckets.get(start + index).getTotalXp();
                int barHeight = Math.max(1,
                        (int) Math.round(chartHeight * (xp / (double) max)));
                int x = left + (int) Math.floor(index * step);
                g.fillRect(x, bottom - barHeight, barWidth, barHeight);
            }

            g.setColor(StrategistTheme.MUTED_TEXT);
            g.drawString("XP / 5 min", left, height - 4);
        }
        finally
        {
            g.dispose();
        }
    }

    private static long totalXp(List<ProgressTimeBucket> values)
    {
        long result = 0L;
        for (ProgressTimeBucket value : values) result += value.getTotalXp();
        return result;
    }
}
