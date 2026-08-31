package com.udderlywet.osrsstrategist;

import java.awt.Component;
import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.cluescrolls.ClueScrollPlugin;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Protects the display rename without invalidating existing local profiles. */
public class BrandingCompatibilityTest
{
    @Test
    public void pluginAndSidebarUseGielinorCompassBrand() throws Exception
    {
        PluginDescriptor descriptor = OsrsStrategistPlugin.class
                .getAnnotation(PluginDescriptor.class);
        assertEquals("Gielinor Compass", descriptor.name());
        PluginDependency dependency = OsrsStrategistPlugin.class
                .getAnnotation(PluginDependency.class);
        assertEquals(ClueScrollPlugin.class, dependency.value());

        OsrsStrategistPanel panel = new OsrsStrategistPanel(
                (id, action) -> { }, null);
        String text = allText(panel);
        assertTrue(text.contains("GIELINOR COMPASS"));
        assertTrue(text.contains("DO NEXT"));
        assertFalse(text.contains("OSRS STRATEGIST"));

        String metadata = new String(Files.readAllBytes(
                Paths.get("runelite-plugin.properties")), StandardCharsets.UTF_8);
        assertTrue(metadata.contains("displayName=Gielinor Compass"));
        assertFalse(metadata.contains("displayName=OSRS Strategist"));
    }

    @Test
    public void stableConfigurationAndProfileGroupsRemainCompatible()
    {
        assertEquals("osrs-strategist", OsrsStrategistConfig.GROUP);
        assertEquals("osrs-strategist-profile", AccountProfileStore.GROUP);
        assertEquals("osrs-strategist-profile", AccountAccessMemoryStore.GROUP);
        assertEquals("osrs-strategist-profile", FarmingRunStateStore.GROUP);
        assertEquals("osrs-strategist-profile",
                AccountProfileStore.GROUP);
    }

    private static String allText(Component component)
    {
        StringBuilder result = new StringBuilder();
        if (component instanceof javax.swing.JLabel)
            result.append(((javax.swing.JLabel) component).getText()).append('\n');
        if (component instanceof javax.swing.AbstractButton)
            result.append(((javax.swing.AbstractButton) component).getText()).append('\n');
        if (component instanceof Container)
            for (Component child : ((Container) component).getComponents())
                result.append(allText(child));
        return result.toString();
    }
}
