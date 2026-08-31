package compass;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import net.runelite.client.ui.ColorScheme;

/**
 * Central visual language for the Compass sidebar.
 *
 * <p>The goal is Text.get(1706): charcoal surfaces,
 * muted gold accents, and restrained status colors. Keeping colors here avoids
 * turning individual panels into a pile of one-off styling decisions.</p>
 */
public final class StrategistTheme
{
    public static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
    public static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
    public static final Color CARD_HOVER = ColorScheme.DARK_GRAY_HOVER_COLOR;

    public static final Color GOLD = new Color(211, 166, 67);
    public static final Color GOLD_SOFT = new Color(181, 142, 63);
    public static final Color TEXT = new Color(220, 220, 220);
    public static final Color MUTED_TEXT = new Color(160, 160, 160);
    public static final Color SUCCESS = new Color(112, 184, 113);
    public static final Color WARNING = new Color(214, 166, 82);
    public static final Color DANGER = new Color(196, 96, 96);
    public static final Color DIVIDER = new Color(67, 67, 67);

    private StrategistTheme()
    {
    }

    public static Border cardBorder()
    {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIVIDER),
                BorderFactory.createEmptyBorder(9, 9, 9, 9)
        );
    }

    public static Border highlightedCardBorder()
    {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD_SOFT),
                BorderFactory.createEmptyBorder(9, 9, 9, 9)
        );
    }
}
