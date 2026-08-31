package com.udderlywet.osrsstrategist;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.*;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Movable on-game decision summary.
 *
 * <p>The full planner graph remains internal; players see only the selected
 * goal relationship, reason, blocker, current step, and next unlock when it is
 * useful.</p>
 */
public class RecommendationDetailsOverlay extends OverlayPanel
{
    private static final int PANEL_WIDTH = 340;
    private static final int TEXT_WIDTH = 310;
    private Recommendation recommendation;
    private GoalRecommendationContext goalContext;

    @Inject
    public RecommendationDetailsOverlay(OsrsStrategistPlugin plugin)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_RIGHT);
        setMovable(true);
        panelComponent.setPreferredSize(new Dimension(PANEL_WIDTH, 0));
    }

    public synchronized void showRecommendation(Recommendation recommendation)
    {
        showRecommendation(recommendation, null);
    }

    public synchronized void showRecommendation(Recommendation recommendation,
            GoalRecommendationContext goalContext)
    {
        this.recommendation = recommendation;
        this.goalContext = goalContext;
    }

    public synchronized void clear()
    {
        recommendation = null;
        goalContext = null;
    }

    public synchronized boolean hasRecommendation()
    {
        return recommendation != null;
    }

    @Override
    public synchronized Dimension render(Graphics2D graphics)
    {
        if (recommendation == null) return null;

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Compass Details")
                .color(StrategistTheme.GOLD)
                .build());

        FontMetrics metrics = graphics.getFontMetrics();
        for (Presentation.Section section
                : Presentation.detailsSections(
                        recommendation, goalContext))
        {
            addLine(section.getHeading(), StrategistTheme.GOLD_SOFT);
            for (String wrapped : wrap(section.getValue(), metrics, TEXT_WIDTH))
                addLine(wrapped, stateColor(section.getValue()));
        }
        return super.render(graphics);
    }

    private void addLine(String text, Color color)
    {
        panelComponent.getChildren().add(LineComponent.builder()
                .left(text)
                .leftColor(color)
                .build());
    }

    private static Color stateColor(String line)
    {
        if (line.startsWith("✓")) return StrategistTheme.SUCCESS;
        if (line.startsWith("✕") || line.startsWith("Blocked")) return StrategistTheme.DANGER;
        if (line.startsWith("?")) return StrategistTheme.WARNING;
        return StrategistTheme.TEXT;
    }

    static List<String> wrap(String text, FontMetrics metrics, int maxWidth)
    {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty())
        {
            result.add("");
            return result;
        }
        if (metrics == null || maxWidth <= 0)
        {
            result.add(text);
            return result;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words)
        {
            if (word.isEmpty()) continue;
            if (line.length() == 0)
            {
                appendLongWord(result, line, word, metrics, maxWidth);
                continue;
            }

            String candidate = line + " " + word;
            if (metrics.stringWidth(candidate) <= maxWidth)
            {
                line.append(' ').append(word);
            }
            else
            {
                result.add(line.toString());
                line.setLength(0);
                appendLongWord(result, line, word, metrics, maxWidth);
            }
        }
        if (line.length() > 0) result.add(line.toString());
        if (result.isEmpty()) result.add("");
        return result;
    }

    private static void appendLongWord(
            List<String> completed,
            StringBuilder current,
            String word,
            FontMetrics metrics,
            int maxWidth)
    {
        if (metrics.stringWidth(word) <= maxWidth)
        {
            current.append(word);
            return;
        }

        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < word.length(); i++)
        {
            char ch = word.charAt(i);
            if (chunk.length() > 0
                    && metrics.stringWidth(chunk.toString() + ch) > maxWidth)
            {
                completed.add(chunk.toString());
                chunk.setLength(0);
            }
            chunk.append(ch);
        }
        current.append(chunk);
    }

}
