package compass;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UiTextStressTest
{
    @Test
    public void detailsAndGuidanceWrapLongNamesQuantitiesAndUnbrokenTokens()
    {
        BufferedImage image = new BufferedImage(800, 600,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try
        {
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
            graphics.setFont(font);
            FontMetrics metrics = graphics.getFontMetrics(font);
            String text = "Recipe for Disaster - Another Cook's Quest "
                    + "requires 2,147,483,647 carefully-accounted supplies "
                    + "and SupercalifragilisticexpialidociousWithoutBreaks";
            assertWrapped(RecommendationDetailsOverlay.wrap(text, metrics, 310),
                    metrics, 310);
            assertWrapped(MethodGuidanceOverlay.wrap(text, metrics, 230),
                    metrics, 230);
        }
        finally
        {
            graphics.dispose();
        }
    }

    @Test
    public void wrappingNeverDropsContentOrReturnsBlankForContent()
    {
        BufferedImage image = new BufferedImage(300, 200,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try
        {
            FontMetrics metrics = graphics.getFontMetrics();
            List<String> lines = RecommendationDetailsOverlay.wrap(
                    "CHECK_NEEDED UIM retrieval-only death-storage setup", metrics,
                    90);
            assertFalse(lines.isEmpty());
            assertFalse(String.join("", lines).trim().isEmpty());
            // A narrow renderer may split CHECK_NEEDED itself depending on the
            // runner's installed font. Rejoining without inserted whitespace
            // verifies that wrapping preserved the token rather than dropping it.
            assertTrue(String.join("", lines).contains("CHECK_NEEDED"));
        }
        finally
        {
            graphics.dispose();
        }
    }

    private static void assertWrapped(List<String> lines, FontMetrics metrics,
            int width)
    {
        assertFalse(lines.isEmpty());
        for (String line : lines)
            assertTrue("overflow: " + line,
                    metrics.stringWidth(line) <= width);
    }
}
