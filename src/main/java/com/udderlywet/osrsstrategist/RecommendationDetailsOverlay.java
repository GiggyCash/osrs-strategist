package com.udderlywet.osrsstrategist;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Movable on-game expanded recommendation view.
 *
 * <p>The fixed RuneLite sidebar carries only the compact action. Details uses
 * game-screen space where long instructions, supply lists, and readiness checks
 * can be read without squeezing them into a 225px plugin panel.</p>
 */
public class RecommendationDetailsOverlay extends OverlayPanel
{
    private static final int PANEL_WIDTH = 340;
    private static final int TEXT_WIDTH = 310;
    private static final Set<String> HEADINGS = new HashSet<>(Arrays.asList(
            "NEXT STEP",
            "BEST METHOD",
            "DO THIS",
            "SUPPLIES",
            "WHERE",
            "NOTE",
            "HOW",
            "READINESS",
            "WHY IT MATTERS",
            "STATUS",
            "NEEDS INFO",
            "NOT READY YET",
            "METHOD UNAVAILABLE",
            "BLOCKED"));

    private Recommendation recommendation;

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
        this.recommendation = recommendation;
    }

    public synchronized void clear()
    {
        recommendation = null;
    }

    @Override
    public synchronized Dimension render(Graphics2D graphics)
    {
        if (recommendation == null) return null;

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("OSRS Strategist Details")
                .color(StrategistTheme.GOLD)
                .build());

        FontMetrics metrics = graphics.getFontMetrics();
        for (String line : wrap(safe(recommendation.getTitle()), metrics, TEXT_WIDTH))
        {
            addLine(line, StrategistTheme.TEXT);
        }
        addLine("", StrategistTheme.TEXT);

        String details = RecommendationPresentation.detailedText(recommendation);
        String[] logicalLines = details.split("\\n", -1);
        for (String logical : logicalLines)
        {
            String trimmed = logical.trim();
            if (trimmed.isEmpty())
            {
                addLine("", StrategistTheme.TEXT);
                continue;
            }

            Color color = HEADINGS.contains(trimmed)
                    ? StrategistTheme.GOLD_SOFT
                    : stateColor(trimmed);
            for (String wrapped : wrap(logical, metrics, TEXT_WIDTH))
            {
                addLine(wrapped, color);
            }
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

    private static String safe(String value)
    {
        return value == null ? "" : value;
    }
}
