package compass;
import static java.lang.Math.*;
import static java.util.Collections.*;

import static compass.Text.get;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.JPanel;

/** Lightweight, dependency-free XP-over-time chart for RuneLite's sidebar. */
public final class ProgressChartPanel extends JPanel
{
    private static final int DEFAULT_HEIGHT = 112;
    List<ProgressTimeBucket> buckets = emptyList();

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
        buckets = values == null ? emptyList()
                : unmodifiableList(new ArrayList<>(values));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        var g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            var width = getWidth();
            var height = getHeight();
            var left = 7;
            var right = max(left + 1, width - 7);
            var top = 8;
            var bottom = max(top + 1, height - 18);

            if (buckets.isEmpty() || totalXp(buckets) <= 0L)
            {
                var message = get(1155);
                var metrics = g.getFontMetrics();
                var x = max(5, (width - metrics.stringWidth(message)) / 2);
                g.setColor(StrategistTheme.MUTED_TEXT);
                g.drawString(message, x, max(metrics.getAscent() + 3,
                        height / 2));
                return;
            }

            g.setColor(StrategistTheme.DIVIDER);
            g.setStroke(new BasicStroke(1f));
            g.drawLine(left, bottom, right, bottom);

            var available = max(1, right - left);
            var displayed = min(buckets.size(), available);
            var start = buckets.size() - displayed;
            var max = 1;
            for (int i = start; i < buckets.size(); i++)
                max = max(max, buckets.get(i).getTotalXp());

            var step = available / (double) displayed;
            var barWidth = max(1, (int) floor(step * 0.72));
            var chartHeight = max(1, bottom - top);
            g.setColor(StrategistTheme.GOLD);
            for (int index = 0; index < displayed; index++)
            {
                var xp = buckets.get(start + index).getTotalXp();
                int barHeight = max(1,
                        (int) round(chartHeight * (xp / (double) max)));
                var x = left + (int) floor(index * step);
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
        var result = 0L;
        for (ProgressTimeBucket value : values) result += value.getTotalXp();
        return result;
    }
}
