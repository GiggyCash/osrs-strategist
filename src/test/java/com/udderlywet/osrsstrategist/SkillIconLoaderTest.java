package com.udderlywet.osrsstrategist;

import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class SkillIconLoaderTest
{
    @Test
    public void callbackFromPriorRecommendationCannotOverwriteClearedIcon()
            throws Exception
    {
        SkillIconLoader loader = new SkillIconLoader(null);
        JLabel label = new JLabel();

        loader.clear(label);
        loader.cacheAndApply(label, "MINING:26",
                new BufferedImage(25, 25, BufferedImage.TYPE_INT_ARGB), 26);
        SwingUtilities.invokeAndWait(() -> { });

        assertNull(label.getIcon());
    }
}
