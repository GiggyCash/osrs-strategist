package compass;
import static java.util.Collections.*;

import java.awt.Color;
import java.awt.FontMetrics;
import java.util.*;
import net.runelite.client.ui.overlay.components.LineComponent;

/** Shared measured text and line rendering for Compass overlays. */
final class OverlayUi
{
    private OverlayUi() { }

    static LineComponent line(String text, Color color)
    {
        return LineComponent.builder().left(text).leftColor(color).build();
    }

    static List<String> wrap(String text, FontMetrics metrics, int width)
    {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return singletonList("");
        var current = new StringBuilder();
        for (String word : text.trim().split("\\s+"))
        {
            if (current.length() > 0
                    && metrics.stringWidth(current + " " + word) > width)
            {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (metrics.stringWidth(word) <= width)
            {
                if (current.length() > 0) current.append(' ');
                current.append(word);
                continue;
            }
            for (int start = 0; start < word.length();)
            {
                int end = start + 1;
                while (end <= word.length()
                        && metrics.stringWidth(word.substring(start, end)) <= width) end++;
                end = Math.max(start + 1, end - 1);
                if (current.length() > 0)
                {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                if (end < word.length()) lines.add(word.substring(start, end));
                else current.append(word, start, end);
                start = end;
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines.isEmpty() ? singletonList(text) : lines;
    }
}
