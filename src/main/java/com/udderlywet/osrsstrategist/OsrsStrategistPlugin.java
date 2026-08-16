package com.udderlywet.osrsstrategist;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
        name = "OSRS Strategist",
        description = "Adaptive progression strategy for every OSRS account type",
        tags = {"strategy", "progression", "maxing", "ironman", "uim", "gim", "clues", "farming"}
)
public class OsrsStrategistPlugin extends Plugin
{
    @Inject
    private OsrsStrategistConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    private NavigationButton navButton;
    private OsrsStrategistPanel panel;

    @Provides
    OsrsStrategistConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(OsrsStrategistConfig.class);
    }

    @Override
    protected void startUp()
    {
        panel = new OsrsStrategistPanel();

        BufferedImage icon = createTemporaryIcon();

        navButton = NavigationButton.builder()
                .tooltip("OSRS Strategist")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown()
    {
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
        }

        panel = null;
        navButton = null;
    }

    private BufferedImage createTemporaryIcon()
    {
        BufferedImage image =
                new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = image.createGraphics();

        graphics.setColor(new Color(60, 45, 30));
        graphics.fillRect(0, 0, 16, 16);

        graphics.setColor(new Color(212, 167, 44));
        graphics.drawOval(1, 1, 13, 13);
        graphics.drawLine(8, 3, 8, 13);
        graphics.drawLine(3, 8, 13, 8);

        graphics.dispose();

        return image;
    }
}